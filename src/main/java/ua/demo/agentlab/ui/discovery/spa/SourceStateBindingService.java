package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.spa.model.RequirementStateTransition;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves where a requirement starts and admits only relevant, safe candidates to live verification. */
public final class SourceStateBindingService {
    private static final Set<String> GENERIC_WORDS = Set.of(
            "application", "authenticated", "confirmed", "discovery", "module", "navigation", "open",
            "page", "route", "source", "target", "user", "from", "the", "and", "area", "option"
    );

    public SourceStateBindingBundle bind(ProjectProfile profile, List<StructuredBehaviorContract> contracts,
                                         UiInteractionInventory inventory, SpaInventoryConfig config) {
        if (profile == null || inventory == null || config == null) {
            return new SourceStateBindingBundle(SourceStateBindingBundle.SCHEMA_VERSION, null, List.of(),
                    List.of("source-state-binding:missing-input"));
        }
        List<SourceStateBinding> bindings = (contracts == null ? List.<StructuredBehaviorContract>of() : contracts).stream()
                .map(contract -> bindOne(profile, contract, inventory.pages(), config))
                .toList();
        return new SourceStateBindingBundle(SourceStateBindingBundle.SCHEMA_VERSION,
                inventory.pages().isEmpty() ? null : inventory.pages().get(0).runMetadata(), bindings,
                List.of("source-state-binding:requirement-scoped", "candidate-admission-threshold=" + config.minLiveVerificationScore()));
    }

    public SourceStateBindingBundle rebindConfirmedTransitions(
            SourceStateBindingBundle current,
            LiveTransitionDiscovery discovery,
            UiInteractionInventory inventory
    ) {
        if (current == null || discovery == null || inventory == null) {
            return current;
        }
        List<SourceStateBinding> rebound = current.bindings().stream()
                .map(binding -> confirmedTransition(binding, discovery, inventory))
                .toList();
        List<String> trace = new ArrayList<>(current.sourceTrace());
        trace.add("source-state-binding:confirmed-transition-rebind");
        return new SourceStateBindingBundle(current.schemaVersion(), current.runMetadata(), rebound, distinct(trace));
    }

    private SourceStateBinding confirmedTransition(
            SourceStateBinding binding,
            LiveTransitionDiscovery discovery,
            UiInteractionInventory inventory
    ) {
        RequirementStateTransition transition = discovery.transitions().stream()
                .filter(candidate -> candidate.confirmed()
                        && candidate.requirementId().equalsIgnoreCase(binding.requirementId())
                        && !candidate.sourcePageId().isBlank()
                        && !candidate.sourceRoute().isBlank())
                .findFirst()
                .orElse(null);
        if (transition == null) {
            return binding;
        }
        UiInteractionPage source = inventory.pages().stream()
                .filter(page -> page.pageId().equalsIgnoreCase(transition.sourcePageId())
                        || routeMatches(page.route(), transition.sourceRoute()))
                .findFirst()
                .orElse(null);
        List<String> componentIds = source == null ? binding.componentIds() : source.components().stream()
                .filter(component -> component.locators().stream()
                        .anyMatch(locator -> locator.locatorId().equals(transition.locatorId()))
                        || component.actions().stream()
                        .anyMatch(action -> action.actionId().equals(transition.actionId())))
                .map(SemanticComponentInventory::componentId)
                .toList();
        return new SourceStateBinding(
                binding.requirementId(),
                binding.capability(),
                transition.sourcePageId(),
                transition.sourceRoute(),
                binding.targetHint(),
                componentIds.isEmpty() ? binding.componentIds() : componentIds,
                transition.locatorId().isBlank() ? binding.candidateLocatorIds() : List.of(transition.locatorId()),
                transition.actionId().isBlank() ? binding.candidateActionIds() : List.of(transition.actionId()),
                true,
                List.of()
        );
    }

    private SourceStateBinding bindOne(ProjectProfile profile, StructuredBehaviorContract contract,
                                       List<UiInteractionPage> pages, SpaInventoryConfig config) {
        List<String> review = new ArrayList<>(contract.reviewReasons());
        UiInteractionPage source = resolveSourcePage(profile, contract, pages);
        String targetHint = targetHint(contract);
        if (source == null) {
            review.add("No current-run source state matches the confirmed profile/discovery context.");
            return new SourceStateBinding(contract.requirementId(), contract.capability(), "", "", targetHint,
                    List.of(), List.of(), List.of(), false, distinct(review));
        }

        Set<ComponentType> requestedTypes = componentTypes(contract);
        Set<String> targetTokens = meaningfulTokens(targetHint + " " + String.join(" ", contract.actions()));
        Set<String> requestedActionIntents = requestedActionIntents(contract);
        List<SemanticComponentInventory> components = source.components().stream()
                .filter(component -> requestedTypes.isEmpty() || requestedTypes.contains(component.type()))
                .toList();
        Set<String> actionRequiredLocatorIds = components.stream()
                .flatMap(component -> component.actions().stream())
                .filter(action -> requestedActionIntents.contains(action.intent().toUpperCase(Locale.ROOT)))
                .flatMap(action -> action.requiredLocatorIds().stream())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, CandidateLocatorEvidence> locatorsBySelector = new LinkedHashMap<>();
        components.stream()
                .flatMap(component -> component.locators().stream())
                .filter(locator -> candidateRelevant(locator, targetTokens)
                        || actionRequiredLocatorIds.contains(locator.locatorId()))
                .filter(locator -> admitted(locator, config))
                .sorted(Comparator.comparingDouble(CandidateLocatorEvidence::qualityScore).reversed()
                        .thenComparing(CandidateLocatorEvidence::locatorId))
                .forEach(locator -> locatorsBySelector.putIfAbsent(locator.strategy().toLowerCase(Locale.ROOT)
                        + "|" + locator.value(), locator));
        List<String> locatorIds = locatorsBySelector.values().stream().map(CandidateLocatorEvidence::locatorId).toList();
        Set<String> admittedLocators = Set.copyOf(locatorIds);
        boolean actionRequired = contract.actions().stream()
                .anyMatch(action -> !normalize(action).startsWith("inspect") && !isRouteOpenAction(action));
        List<String> actionIds = actionRequired ? components.stream()
                .flatMap(component -> component.actions().stream())
                .filter(action -> actionRelevant(action, admittedLocators, targetTokens,
                        contract.capability(), requestedActionIntents))
                .filter(action -> action.confidence() >= config.minLiveVerificationScore())
                .sorted(Comparator.comparingDouble(CandidateActionEvidence::confidence).reversed()
                        .thenComparing(CandidateActionEvidence::actionId))
                .map(CandidateActionEvidence::actionId)
                .distinct()
                .toList() : List.of();
        if (locatorIds.isEmpty()) {
            review.add("No requirement-relevant candidate locator passed live-verification admission policy.");
        }
        if (actionRequired && actionIds.isEmpty()) {
            review.add("No requirement-relevant candidate action is available for live transition discovery.");
        }
        boolean eligible = locatorIds.size() > 0 && (!actionRequired || !actionIds.isEmpty());
        return new SourceStateBinding(contract.requirementId(), contract.capability(), source.pageId(), source.route(),
                targetHint, components.stream().map(SemanticComponentInventory::componentId).toList(), locatorIds,
                actionIds, eligible, distinct(review));
    }

    private UiInteractionPage resolveSourcePage(ProjectProfile profile, StructuredBehaviorContract contract,
                                               List<UiInteractionPage> pages) {
        String sourceRoute = contextValue(contract.targetContext(), "sourceRoute");
        String resolvedRoute = resolveProfileRoute(profile, sourceRoute);
        if (!resolvedRoute.isBlank()) {
            return pages.stream().filter(page -> routeMatches(page.route(), resolvedRoute)).findFirst().orElse(null);
        }
        Set<String> sourceTokens = meaningfulTokens(sourceRoute + " " + contextValue(contract.targetContext(), "sourcePage"));
        if (!sourceTokens.isEmpty()) {
            List<UiInteractionPage> matches = pages.stream().filter(page -> tokensMatch(page, sourceTokens)).toList();
            if (matches.size() == 1) return matches.get(0);
        }
        String targetRoute = resolveProfileRoute(profile, contextValue(contract.targetContext(), "targetRoute"));
        if (!targetRoute.isBlank()) {
            return pages.stream().filter(page -> routeMatches(page.route(), targetRoute)).findFirst().orElse(null);
        }
        // The authenticated route is a last-resort source only for a true navigation requirement
        // without an explicit source or target. It must not steal a public home-page requirement.
        if (normalize(contract.capability()).equals("module_navigation") && !profile.authenticatedRoute().isBlank()) {
            return pages.stream().filter(page -> routeMatches(page.route(), profile.authenticatedRoute())).findFirst().orElse(null);
        }
        Set<String> targetTokens = meaningfulTokens(contextValue(contract.targetContext(), "targetPage"));
        List<UiInteractionPage> matches = pages.stream().filter(page -> tokensMatch(page, targetTokens)).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean admitted(CandidateLocatorEvidence locator, SpaInventoryConfig config) {
        boolean unique = locator.globalMatchCount() == 1 || locator.componentMatchCount() == 1;
        boolean safe = locator.risks().stream().map(this::normalize).noneMatch(risk ->
                risk.contains("external") || risk.contains("absolute") || risk.contains("hidden") || risk.contains("missing"));
        return locator.qualityScore() >= config.minLiveVerificationScore() && locator.sameOrigin() && unique && safe;
    }

    private boolean candidateRelevant(CandidateLocatorEvidence locator, Set<String> targetTokens) {
        if (targetTokens.isEmpty()) return true;
        return evidenceMatchesTokens(locator.locatorId() + " " + locator.elementId() + " " + locator.value(),
                targetTokens, false);
    }

    private boolean actionRelevant(CandidateActionEvidence action, Set<String> locatorIds,
                                   Set<String> targetTokens, String capability,
                                   Set<String> requestedActionIntents) {
        if (action.requiredLocatorIds().stream().noneMatch(locatorIds::contains)) return false;
        String intent = normalize(action.intent());
        String normalizedCapability = normalize(capability);
        if (normalizedCapability.equals("module_navigation") && !intent.equals("click")) return false;
        if (!requestedActionIntents.isEmpty() && !requestedActionIntents.contains(intent.toUpperCase(Locale.ROOT))) {
            return false;
        }
        if (targetTokens.isEmpty()) return true;
        String evidence = action.actionId() + " " + action.targetElementId() + " " + String.join(" ", action.sourceTrace());
        return evidenceMatchesTokens(evidence, targetTokens, false)
                || action.requiredLocatorIds().stream().anyMatch(locatorIds::contains);
    }

    private Set<ComponentType> componentTypes(StructuredBehaviorContract contract) {
        String value = normalize(contextValue(contract.targetContext(), "componentCapability") + " " + contract.capability());
        Set<ComponentType> result = new LinkedHashSet<>();
        if (value.contains("navigation") || value.contains("module_navigation")) result.add(ComponentType.NAVIGATION);
        if (value.contains("filter")) result.add(ComponentType.FILTER_PANEL);
        if (value.contains("results_collection") || value.contains("record_list")) {
            result.add(ComponentType.RESULTS_COLLECTION);
            result.add(ComponentType.TABLE);
        }
        if (value.contains("modal")) result.add(ComponentType.MODAL);
        if (value.contains("user_menu") || value.contains("logout")) {
            result.add(ComponentType.USER_MENU);
            // The opener commonly belongs to a persistent header while the revealed controls
            // belong to the menu component. Both are part of one typed interaction flow.
            result.add(ComponentType.HEADER);
        }
        if (value.contains("form") || value.contains("authentication")) result.add(ComponentType.FORM);
        return result;
    }

    private Set<String> requestedActionIntents(StructuredBehaviorContract contract) {
        Set<String> intents = new LinkedHashSet<>();
        for (String raw : contract.actions()) {
            String action = normalize(raw);
            if (isRouteOpenAction(raw)) continue;
            if (action.startsWith("enter_") || action.startsWith("type_")) intents.add("TYPE");
            if (action.startsWith("submit_")) intents.add("SUBMIT_FORM");
            if (action.contains("user_menu") && action.startsWith("open_")) intents.add("OPEN_MENU");
            if (action.contains("logout") || action.contains("sign_out")) intents.add("LOGOUT");
            if ((action.startsWith("click_") || action.startsWith("open_"))
                    && !action.contains("user_menu") && !action.contains("logout")) intents.add("CLICK");
            if (action.startsWith("select_")) intents.add("SELECT");
        }
        String context = normalize(contract.targetContext());
        if (intents.contains("LOGOUT") && context.contains("user_menu")) {
            intents.add("OPEN_MENU");
        }
        return intents;
    }

    private boolean isRouteOpenAction(String raw) {
        String action = normalize(raw);
        return action.startsWith("open_the_target_page")
                || action.startsWith("open_target_page")
                || action.startsWith("open_the_application")
                || action.startsWith("open_application");
    }

    private String targetHint(StructuredBehaviorContract contract) {
        return String.join(" ", contextValue(contract.targetContext(), "targetPage"),
                contextValue(contract.targetContext(), "targetRoute"), String.join(" ", contract.actions())).trim();
    }

    private String resolveProfileRoute(ProjectProfile profile, String value) {
        String normalized = normalize(value);
        if (normalized.equals("project_profile_authenticatedroute")) return profile.authenticatedRoute();
        if (normalized.equals("project_profile_loginroute")) return profile.loginRoute();
        String route = value == null ? "" : value.trim();
        return route.startsWith("/") ? route : "";
    }

    private boolean tokensMatch(UiInteractionPage page, Set<String> tokens) {
        if (tokens.isEmpty()) return false;
        String evidence = page.pageId() + " " + page.pageName() + " " + page.route() + " " + page.capability();
        return evidenceMatchesTokens(evidence, tokens, true);
    }

    private boolean evidenceMatchesTokens(String evidence, Set<String> expectedTokens, boolean requireAll) {
        String normalizedEvidence = normalize(evidence);
        Set<String> evidenceTokens = meaningfulTokens(evidence);
        java.util.function.Predicate<String> matches = token -> normalizedEvidence.contains(token)
                || evidenceTokens.contains(token);
        return requireAll ? expectedTokens.stream().allMatch(matches) : expectedTokens.stream().anyMatch(matches);
    }

    private Set<String> meaningfulTokens(String value) {
        Set<String> result = new LinkedHashSet<>();
        for (String token : normalize(value).split("_+")) {
            String canonical = canonicalToken(token);
            if (canonical.length() >= 4 && !GENERIC_WORDS.contains(canonical)) result.add(canonical);
        }
        return result;
    }

    private String canonicalToken(String token) {
        if (token == null) return "";
        if (token.endsWith("ies") && token.length() > 4) {
            return token.substring(0, token.length() - 3) + "y";
        }
        if (token.endsWith("s") && token.length() > 4 && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    private String contextValue(String context, String key) {
        return BehaviorTargetContext.parse(context).value(key);
    }

    private boolean routeMatches(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        return !a.isBlank() && !b.isBlank() && (a.equals(b) || a.endsWith(b) || b.endsWith(a));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private List<String> distinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }
}
