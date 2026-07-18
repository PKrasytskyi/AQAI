package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorAssertion;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorStep;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentFlowType;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TypedComponentFlow;
import ua.demo.agentlab.ui.discovery.spa.binding.BehaviorBindingResultAssembler;
import ua.demo.agentlab.ui.discovery.spa.binding.BehaviorExecutabilityGate;
import ua.demo.agentlab.ui.discovery.spa.binding.BehaviorSourceStateResolver;
import ua.demo.agentlab.ui.discovery.spa.binding.BehaviorTargetStateResolver;
import ua.demo.agentlab.ui.discovery.spa.binding.ActionSequenceBinder;
import ua.demo.agentlab.ui.discovery.spa.binding.PostconditionBindingService;
import ua.demo.agentlab.ui.discovery.spa.binding.ScenarioDataBindingService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates executable SPA contracts only when every step and assertion is backed by current-run,
 * live-confirmed inventory evidence. It deliberately does not promote candidate or cached facts.
 */
public final class StructuredSpaBehaviorBindingService {

    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([^}]+)}");
    private final ScenarioDataBindingService dataBinding = new ScenarioDataBindingService();
    private final BehaviorSourceStateResolver sourceStateResolver = new BehaviorSourceStateResolver();
    private final BehaviorTargetStateResolver targetStateResolver = new BehaviorTargetStateResolver(sourceStateResolver);
    private final ActionSequenceBinder actionSequenceBinder = new ActionSequenceBinder();
    private final PostconditionBindingService postconditionBinding = new PostconditionBindingService();
    private final BehaviorExecutabilityGate executabilityGate = new BehaviorExecutabilityGate();
    private final BehaviorBindingResultAssembler resultAssembler = new BehaviorBindingResultAssembler();

    public List<BoundSpaBehaviorContract> bind(
            List<StructuredBehaviorContract> contracts,
            SpaInventoryBundle inventory,
            SpaTargetedVerificationResult verification
    ) {
        return bind(contracts, inventory, verification, null);
    }

    public List<BoundSpaBehaviorContract> bind(
            List<StructuredBehaviorContract> contracts,
            SpaInventoryBundle inventory,
            SpaTargetedVerificationResult verification,
            SourceStateBindingBundle sourceBindings
    ) {
        if (contracts == null || inventory == null || verification == null) {
            return List.of();
        }
        EvidenceIndex evidence = EvidenceIndex.from(inventory, verification);
        List<TypedComponentFlow> flows = new TypedComponentFlowBuilder().build(inventory).flows();
        Map<String, SourceStateBinding> sources = sourceBindings == null ? Map.of() : sourceBindings.bindings().stream()
                .collect(java.util.stream.Collectors.toMap(
                        source -> source.requirementId().toLowerCase(Locale.ROOT),
                        source -> source,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        return contracts.stream().map(contract -> bind(contract, evidence, flows,
                sources.get(contract.requirementId().toLowerCase(Locale.ROOT)))).toList();
    }

    private BoundSpaBehaviorContract bind(
            StructuredBehaviorContract contract,
            EvidenceIndex evidence,
            List<TypedComponentFlow> flows,
            SourceStateBinding sourceBinding
    ) {
        List<String> review = new ArrayList<>(contract.reviewReasons());
        var data = dataBinding.resolve(contract.dataRequirements());
        Map<String, String> values = data.values();
        review.addAll(data.reviewReasons());
        SpaPageInventory page = targetStateResolver.resolve(contract, evidence.pages(), sourceBinding).orElse(null);
        if (page == null) {
            review.add("No current-run page matches target capability/context '" + contract.targetContext() + "'.");
            return resultAssembler.empty(contract, values, review);
        }

        List<SemanticComponentInventory> components = componentsFor(contract, page, evidence, sourceBinding);
        if (components.isEmpty()) {
            review.add("No confirmed component matches the requirement capability on page " + page.pageName() + ".");
        }
        List<BoundSpaBehaviorStep> steps = bindSteps(contract, page, components, values, evidence, review);
        List<BoundSpaBehaviorAssertion> assertions = bindAssertions(contract, page, components, evidence, review);
        String flowId = flowFor(contract, page, flows).map(TypedComponentFlow::flowId).orElse("");
        if (flowId.isBlank() && requiredFlowType(contract).isPresent()) {
            review.add("No typed component flow matches capability " + contract.capability() + " on page " + page.pageName() + ".");
        }
        var decision = executabilityGate.evaluate(contract, steps, assertions, review);
        return resultAssembler.assemble(contract, page, flowId, components, steps, assertions, values, decision);
    }

    private List<SemanticComponentInventory> componentsFor(StructuredBehaviorContract contract, SpaPageInventory page,
                                                            EvidenceIndex evidence,
                                                            SourceStateBinding sourceBinding) {
        Set<ComponentType> requested = requestedComponentTypes(contract.targetContext(), contract.capability());
        return page.components().stream()
                .filter(component -> sourceBinding != null && sourceBinding.componentIds().contains(component.componentId())
                        || requested.isEmpty() || requested.contains(component.type()))
                .filter(component -> component.locators().stream().anyMatch(locator -> evidence.confirmedLocatorIds().contains(locator.locatorId())))
                .toList();
    }

    private List<BoundSpaBehaviorStep> bindSteps(
            StructuredBehaviorContract contract,
            SpaPageInventory page,
            List<SemanticComponentInventory> components,
            Map<String, String> values,
            EvidenceIndex evidence,
            List<String> review
    ) {
        return actionSequenceBinder.bind(contract.actions(), rawAction ->
                bindStep(rawAction, page, components, values, evidence), review);
    }

    private ActionSequenceBinder.StepResolution bindStep(
            String rawAction,
            SpaPageInventory page,
            List<SemanticComponentInventory> components,
            Map<String, String> values,
            EvidenceIndex evidence
    ) {
        ActionRequest request = ActionRequest.parse(rawAction, values);
        if (request.kind().isBlank()) {
            return normalize(rawAction).startsWith("inspect")
                    ? ActionSequenceBinder.StepResolution.unbound("")
                    : ActionSequenceBinder.StepResolution.unbound(
                    "No typed action intent could be derived for action '" + rawAction + "'.");
        }
        if (request.kind().equals("OPEN_ROUTE")) {
            return ActionSequenceBinder.StepResolution.bound(
                    new BoundSpaBehaviorStep("OPEN_ROUTE", "", "", "", page.route()));
        }
        ActionBinding actionFirst = actionFirstKind(request.kind())
                ? findActionFirst(components, request, evidence) : null;
        CandidateLocatorEvidence locator = actionFirst == null
                ? findLocator(components, request.target(), evidence) : actionFirst.locator();
        if (locator == null) {
            return ActionSequenceBinder.StepResolution.unbound(
                    "No confirmed locator binding for action '" + rawAction + "'.");
        }
        CandidateActionEvidence action = actionFirst == null
                ? findAction(components, locator.locatorId(), request.kind(), evidence) : actionFirst.action();
        if (action == null) {
            return ActionSequenceBinder.StepResolution.unbound(
                    "No confirmed action binding for locator '" + locator.locatorId()
                            + "' and action '" + request.kind() + "'.");
        }
        return ActionSequenceBinder.StepResolution.bound(new BoundSpaBehaviorStep(
                request.kind(), action.actionId(), locator.locatorId(), request.dataKey(), request.value()));
    }

    private List<BoundSpaBehaviorAssertion> bindAssertions(
            StructuredBehaviorContract contract,
            SpaPageInventory page,
            List<SemanticComponentInventory> components,
            EvidenceIndex evidence,
            List<String> review
    ) {
        return postconditionBinding.bind(contract.assertions(), assertion -> {
            if (routeAssertion(assertion.type())) {
                boolean routeKnown = !page.route().isBlank();
                return new BoundSpaBehaviorAssertion(assertion, "", routeKnown,
                        routeKnown ? "" : "Target route is not confirmed for assertion '" + assertion.target() + "'.");
            }
            CandidateLocatorEvidence locator = findLocator(components, assertion.target(), evidence);
            boolean verifiable = locator != null;
            return new BoundSpaBehaviorAssertion(assertion, verifiable ? locator.locatorId() : "", verifiable,
                    verifiable ? "" : "No confirmed locator binding for assertion target '" + assertion.target() + "'.");
        }, review);
    }

    private CandidateLocatorEvidence findLocator(Collection<SemanticComponentInventory> components, String target,
                                                 EvidenceIndex evidence) {
        return components.stream()
                .flatMap(component -> component.locators().stream()
                        .filter(locator -> evidence.confirmedLocatorIds().contains(locator.locatorId()))
                        .map(locator -> new ScoredLocator(locator,
                                Math.max(matchScore(locator, target), componentMatchScore(component, target)))))
                .filter(candidate -> candidate.score() > 0)
                .max(Comparator.comparingInt(ScoredLocator::score)
                        .thenComparingDouble(candidate -> candidate.locator().qualityScore())
                        .thenComparing(candidate -> candidate.locator().locatorId(), Comparator.reverseOrder()))
                .map(ScoredLocator::locator)
                .orElse(null);
    }

    private int componentMatchScore(SemanticComponentInventory component, String target) {
        String candidate = normalize(component.componentId() + " " + component.name() + " " + component.type());
        String expected = normalize(target);
        if (expected.isBlank()) return 0;
        if (candidate.contains(expected) || expected.contains(candidate)) return 80;
        int matched = (int) expectedTokens(target).stream().filter(candidate::contains).count();
        return matched == 0 ? 0 : 20 + matched;
    }

    private CandidateActionEvidence findAction(
            Collection<SemanticComponentInventory> components,
            String locatorId,
            String requestedKind,
            EvidenceIndex evidence
    ) {
        return components.stream().flatMap(component -> component.actions().stream())
                .filter(action -> action.requiredLocatorIds().contains(locatorId))
                .filter(action -> actionCompatible(action.intent(), requestedKind))
                .filter(action -> evidence.confirmedActionIds().contains(action.actionId()))
                .max(Comparator.comparingDouble(CandidateActionEvidence::confidence)
                        .thenComparing(CandidateActionEvidence::actionId, Comparator.reverseOrder()))
                .orElse(null);
    }

    private ActionBinding findActionFirst(Collection<SemanticComponentInventory> components,
                                          ActionRequest request, EvidenceIndex evidence) {
        return components.stream()
                .flatMap(component -> component.actions().stream()
                        .filter(action -> evidence.confirmedActionIds().contains(action.actionId()))
                        .filter(action -> actionCompatible(action.intent(), request.kind()))
                        .map(action -> new ScoredAction(component, action,
                                actionMatchScore(component, action, request))))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingInt(ScoredAction::score).reversed()
                        .thenComparing(Comparator.comparingDouble(
                                (ScoredAction candidate) -> candidate.action().confidence()).reversed())
                        .thenComparing(candidate -> candidate.action().actionId()))
                .map(candidate -> actionBinding(candidate.component(), candidate.action(), evidence))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private ActionBinding actionBinding(SemanticComponentInventory component, CandidateActionEvidence action,
                                        EvidenceIndex evidence) {
        CandidateLocatorEvidence locator = component.locators().stream()
                .filter(candidate -> action.requiredLocatorIds().contains(candidate.locatorId()))
                .filter(candidate -> evidence.confirmedLocatorIds().contains(candidate.locatorId()))
                .max(Comparator.comparingDouble(CandidateLocatorEvidence::qualityScore)
                        .thenComparing(CandidateLocatorEvidence::locatorId, Comparator.reverseOrder()))
                .orElse(null);
        return locator == null ? null : new ActionBinding(action, locator);
    }

    private int actionMatchScore(SemanticComponentInventory component, CandidateActionEvidence action,
                                 ActionRequest request) {
        String evidence = normalize(component.name() + " " + component.type() + " " + action.actionId()
                + " " + action.targetElementId() + " " + String.join(" ", action.sourceTrace()));
        String target = normalize(request.target());
        int score = target.isBlank() ? 10 : (evidence.contains(target) ? 80
                : (int) expectedTokens(request.target()).stream().filter(evidence::contains).count() * 10);
        String kind = normalize(request.kind());
        if (kind.equals("openmenu")) {
            if (component.type() == ComponentType.HEADER) score += 120;
            if (evidence.contains("trigger") || evidence.contains("dropdown") || evidence.contains("usermenu")) score += 80;
            if (containsAny(evidence, "logout", "about", "support", "password")) score -= 200;
        } else if (kind.equals("logout")) {
            if (component.type() == ComponentType.USER_MENU) score += 120;
            if (evidence.contains("logout") || evidence.contains("signout")) score += 100;
        } else if (kind.equals("submitform")) {
            if (component.type() == ComponentType.FORM) score += 120;
            if (containsAny(evidence, "submit", "login", "authenticate")) score += 100;
            if (containsAny(evidence, "username", "password")) score -= 80;
        }
        return score;
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) return true;
        }
        return false;
    }

    private boolean actionFirstKind(String kind) {
        return Set.of("submitform", "openmenu", "logout").contains(normalize(kind));
    }

    private boolean actionCompatible(String intent, String kind) {
        String normalizedIntent = normalize(intent);
        return switch (normalize(kind)) {
            case "select" -> normalizedIntent.equals("select") || normalizedIntent.equals("filter");
            case "check" -> normalizedIntent.equals("check") || normalizedIntent.equals("click");
            case "uncheck" -> normalizedIntent.equals("uncheck") || normalizedIntent.equals("click");
            case "type" -> normalizedIntent.equals("type") || normalizedIntent.equals("clear");
            case "upload" -> normalizedIntent.equals("upload");
            case "hover" -> normalizedIntent.equals("hover");
            case "setslider" -> normalizedIntent.equals("setslider");
            case "submitform" -> normalizedIntent.equals("submitform") || normalizedIntent.equals("click");
            case "openmenu" -> normalizedIntent.equals("openmenu");
            case "logout" -> normalizedIntent.equals("logout");
            case "click" -> normalizedIntent.equals("click") || normalizedIntent.equals("filter") || normalizedIntent.equals("search")
                    || normalizedIntent.equals("openrecord") || normalizedIntent.equals("openmenu")
                    || normalizedIntent.equals("opennewwindow");
            default -> false;
        };
    }

    private Optional<TypedComponentFlow> flowFor(StructuredBehaviorContract contract, SpaPageInventory page,
                                                  List<TypedComponentFlow> flows) {
        Optional<ComponentFlowType> requiredType = requiredFlowType(contract);
        if (requiredType.isEmpty()) return Optional.empty();
        ComponentFlowType type = requiredType.orElseThrow();
        return flows.stream().filter(flow -> page.pageId().equals(flow.pageId()) && flow.type() == type).findFirst();
    }

    private Optional<ComponentFlowType> requiredFlowType(StructuredBehaviorContract contract) {
        ComponentFlowType type = switch (normalize(contract.capability())) {
            case "modulenavigation" -> ComponentFlowType.MODULE_NAVIGATION;
            case "filter" -> ComponentFlowType.FILTER_RESULTS;
            case "sortcollection", "sort" -> ComponentFlowType.TABLE_SORT;
            case "paginate" -> ComponentFlowType.TABLE_PAGINATION;
            case "openmodal", "confirmaction" -> ComponentFlowType.MODAL_CONFIRMATION;
            default -> null;
        };
        return Optional.ofNullable(type);
    }

    private int matchScore(CandidateLocatorEvidence locator, String target) {
        String candidate = normalize(locator.elementId() + " " + locator.locatorId() + " " + locator.value());
        String expected = normalize(target);
        if (expected.isBlank()) return 0;
        if (candidate.contains(expected) || expected.contains(candidate)) return 100;
        return (int) expectedTokens(target).stream().filter(candidate::contains).count();
    }

    private Set<String> expectedTokens(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : (value == null ? "" : value).split("(?<=[a-z])(?=[A-Z])|[^A-Za-z0-9]+")) {
            String normalized = normalize(token);
            if (normalized.length() > 2 && !Set.of("from", "the", "and", "page", "module", "application", "navigation", "button", "field", "control", "action", "open", "select", "click").contains(normalized)) {
                tokens.add(normalized);
            }
        }
        return tokens;
    }

    private Set<ComponentType> requestedComponentTypes(String context, String capability) {
        Set<ComponentType> result = new LinkedHashSet<>();
        String combined = normalize(context + " " + capability);
        if (combined.contains("navigation") || combined.contains("modulenavigation")) result.add(ComponentType.NAVIGATION);
        if (combined.contains("filter")) result.add(ComponentType.FILTER_PANEL);
        if (combined.contains("resultscollection") || combined.contains("recordlist")) {
            result.add(ComponentType.RESULTS_COLLECTION);
            result.add(ComponentType.TABLE);
        }
        if (combined.contains("modal")) result.add(ComponentType.MODAL);
        if (combined.contains("search")) result.add(ComponentType.SEARCH);
        if (combined.contains("form")) result.add(ComponentType.FORM);
        if (combined.contains("usermenu") || combined.contains("logout")) {
            result.add(ComponentType.USER_MENU);
            result.add(ComponentType.HEADER);
        }
        return result;
    }

    private boolean routeAssertion(String type) {
        String normalized = normalize(type);
        return normalized.equals("routechanged") || normalized.equals("urlcontains") || normalized.equals("routeequals")
                || normalized.equals("authenticatedareavisible")
                || normalized.equals("authenticationsucceeded")
                || normalized.equals("authenticatedareaabsent");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private record ActionRequest(String kind, String target, String dataKey, String value) {
        static ActionRequest parse(String raw, Map<String, String> values) {
            String action = raw == null ? "" : raw.trim();
            String normalized = action.toLowerCase(Locale.ROOT);
            boolean routeOpen = normalized.startsWith("open application")
                    || normalized.startsWith("open the application")
                    || normalized.startsWith("open target page")
                    || normalized.startsWith("open the target page")
                    || normalized.matches("^open (the )?page( at)? \\/.*");
            String kind = normalized.startsWith("deselect ") || normalized.startsWith("uncheck ") ? "UNCHECK"
                    : normalized.startsWith("select ") && normalized.contains("file input") ? "UPLOAD"
                    : normalized.startsWith("select ") && normalized.contains("checkbox") ? "CHECK"
                    : normalized.startsWith("select ") ? "SELECT"
                    : normalized.startsWith("enter ") || normalized.startsWith("type ") ? "TYPE"
                    : normalized.startsWith("upload ") ? "UPLOAD"
                    : normalized.startsWith("hover ") ? "HOVER"
                    : normalized.startsWith("move ") && normalized.contains("slider") ? "SET_SLIDER"
                    : normalized.startsWith("submit ") ? "SUBMIT_FORM"
                    : normalized.contains("logout") || normalized.contains("sign out") ? "LOGOUT"
                    : normalized.startsWith("open ") && normalized.contains("menu") ? "OPEN_MENU"
                    : routeOpen ? "OPEN_ROUTE"
                    : normalized.startsWith("click ") || normalized.startsWith("open ") ? "CLICK" : "";
            Matcher variable = VARIABLE.matcher(action);
            String key = variable.find() ? variable.group(1).trim() : "";
            String withoutValue = action.replaceAll("\\$\\{[^}]+}", "");
            String target = withoutValue.replaceFirst("(?i)^(deselect|uncheck|select|enter|type|click|open|upload|hover|move|submit)\\s+", "").trim();
            target = target.replaceFirst("(?i)^over\\s+", "");
            int fromIndex = target.toLowerCase(Locale.ROOT).indexOf(" from ");
            if (fromIndex >= 0) {
                // SELECT binds to its containing control ("option from dropdown"). Other
                // actions bind to the subject before the contextual source component
                // ("open Recruitment from navigation").
                target = "SELECT".equals(kind)
                        ? target.substring(fromIndex + " from ".length()).trim()
                        : target.substring(0, fromIndex).trim();
            }
            target = target.replaceFirst("(?i)^(the|an|a)\\s+", "").replaceAll("[.]$", "").trim();
            String value = key.isBlank() ? "" : values.getOrDefault(key, "");
            return new ActionRequest(kind, target, key, value);
        }
    }

    private record ActionBinding(CandidateActionEvidence action, CandidateLocatorEvidence locator) {
    }

    private record ScoredAction(SemanticComponentInventory component, CandidateActionEvidence action, int score) {
    }

    private record EvidenceIndex(List<SpaPageInventory> pages, Set<String> confirmedLocatorIds, Set<String> confirmedActionIds) {
        static EvidenceIndex from(SpaInventoryBundle inventory, SpaTargetedVerificationResult verification) {
            Set<String> confirmed = verification.locatorVerifications().stream().filter(TargetedLocatorVerification::verified)
                    .map(TargetedLocatorVerification::locatorId).collect(java.util.stream.Collectors.toSet());
            Set<String> actions = verification.actionVerifications().stream()
                    .filter(TargetedActionVerification::verified)
                    .map(TargetedActionVerification::actionId)
                    .collect(java.util.stream.Collectors.toSet());
            return new EvidenceIndex(inventory.pages(), Set.copyOf(confirmed), Set.copyOf(actions));
        }
    }

    private record ScoredLocator(CandidateLocatorEvidence locator, int score) {
    }
}
