package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.core.data.PropertiesTestDataProvider;
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
        Map<String, String> values = resolveData(contract.dataRequirements(), review);
        SpaPageInventory page = resolvePage(contract, evidence.pages(), sourceBinding);
        if (page == null) {
            review.add("No current-run page matches target capability/context '" + contract.targetContext() + "'.");
            return empty(contract, values, review);
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
        if (steps.isEmpty() && requiresExecutableAction(contract)) {
            review.add("No confirmed executable action binding was produced.");
        }
        if (assertions.stream().anyMatch(assertion -> !assertion.verifiable())) {
            review.add("One or more assertion targets have no confirmed locator or route binding.");
        }

        boolean executable = review.isEmpty() && (!requiresExecutableAction(contract) || !steps.isEmpty());
        return new BoundSpaBehaviorContract(contract.requirementId(), contract.capability(), page.pageId(), page.route(), flowId,
                components.stream().map(SemanticComponentInventory::componentId).toList(), steps, assertions, values,
                executable, List.copyOf(new LinkedHashSet<>(review)));
    }

    private BoundSpaBehaviorContract empty(StructuredBehaviorContract contract, Map<String, String> values, List<String> review) {
        return new BoundSpaBehaviorContract(contract.requirementId(), contract.capability(), "", "", "", List.of(),
                List.of(), List.of(), values, false, List.copyOf(new LinkedHashSet<>(review)));
    }

    private SpaPageInventory resolvePage(StructuredBehaviorContract contract, List<SpaPageInventory> pages,
                                         SourceStateBinding sourceBinding) {
        if (pages == null || pages.isEmpty()) {
            return null;
        }
        if (sourceBinding != null && !sourceBinding.sourcePageId().isBlank()) {
            SpaPageInventory confirmedSource = pages.stream()
                    .filter(page -> page.pageId().equalsIgnoreCase(sourceBinding.sourcePageId())
                            || routeMatches(page.route(), sourceBinding.sourceRoute()))
                    .findFirst()
                    .orElse(null);
            if (confirmedSource != null) {
                return confirmedSource;
            }
        }
        boolean moduleNavigation = normalize(contract.capability()).equals("modulenavigation");
        String targetRoute = explicitRoute(contextValue(contract.targetContext(),
                moduleNavigation ? "sourceRoute" : "targetRoute"));
        String targetPage = semanticTarget(contextValue(contract.targetContext(),
                moduleNavigation ? "sourcePage" : "targetPage"));
        // pageCapability describes the state reached by navigation; it must not be used to
        // reject the source page that owns the navigation action.
        final String expectedCapability = moduleNavigation ? ""
                : normalize(contextValue(contract.targetContext(), "pageCapability"));

        // A structured requirement must bind to a concrete current-run page. Choosing the
        // first page with a matching component silently turns an unconfirmed target into a
        // different page contract (for example, a RECORD_LIST into LoginPage).
        if (!targetRoute.isBlank()) {
            return pages.stream()
                    .filter(page -> routeMatches(page.route(), targetRoute))
                    .filter(page -> expectedCapability.isBlank()
                            || supportsCapability(page, expectedCapability))
                    .sorted(Comparator.comparing(SpaPageInventory::route))
                    .findFirst()
                    .orElse(null);
        }
        if (!targetPage.isBlank()) {
            return pages.stream()
                    .filter(page -> pageMatches(page, targetPage))
                    .filter(page -> expectedCapability.isBlank()
                            || supportsCapability(page, expectedCapability))
                    .sorted(Comparator.comparing(SpaPageInventory::route))
                    .findFirst()
                    .orElse(null);
        }

        // Capability alone is safe only where it identifies one current-run page. Ambiguous
        // capabilities must be reviewed rather than rebound to a page by sort order.
        List<SpaPageInventory> capabilityMatches = pages.stream()
                .filter(page -> !expectedCapability.isBlank()
                        && supportsCapability(page, expectedCapability))
                .sorted(Comparator.comparing(SpaPageInventory::route))
                .toList();
        return capabilityMatches.size() == 1 ? capabilityMatches.get(0) : null;
    }

    private boolean supportsCapability(SpaPageInventory page, String expectedCapability) {
        String expected = normalize(expectedCapability);
        if (expected.isBlank() || page == null) {
            return expected.isBlank();
        }
        return java.util.Arrays.stream(page.capability().split("\\|"))
                .map(this::normalize)
                .anyMatch(expected::equals);
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
        List<BoundSpaBehaviorStep> result = new ArrayList<>();
        for (String rawAction : contract.actions()) {
            ActionRequest request = ActionRequest.parse(rawAction, values);
            if (request.kind().isBlank()) {
                if (!normalize(rawAction).startsWith("inspect")) {
                    review.add("No typed action intent could be derived for action '" + rawAction + "'.");
                }
                continue;
            }
            if (request.kind().equals("OPEN_ROUTE")) {
                result.add(new BoundSpaBehaviorStep("OPEN_ROUTE", "", "", "", page.route()));
                continue;
            }
            CandidateLocatorEvidence locator = findLocator(components, request.target(), evidence);
            if (locator == null) {
                review.add("No confirmed locator binding for action '" + rawAction + "'.");
                continue;
            }
            CandidateActionEvidence action = findAction(components, locator.locatorId(), request.kind(), evidence);
            if (action == null) {
                review.add("No confirmed action binding for locator '" + locator.locatorId() + "' and action '" + request.kind() + "'.");
                continue;
            }
            result.add(new BoundSpaBehaviorStep(request.kind(), action.actionId(), locator.locatorId(), request.dataKey(), request.value()));
        }
        return List.copyOf(result);
    }

    private List<BoundSpaBehaviorAssertion> bindAssertions(
            StructuredBehaviorContract contract,
            SpaPageInventory page,
            List<SemanticComponentInventory> components,
            EvidenceIndex evidence,
            List<String> review
    ) {
        List<BoundSpaBehaviorAssertion> result = new ArrayList<>();
        for (StructuredAssertionRequirement assertion : contract.assertions()) {
            if (routeAssertion(assertion.type())) {
                boolean routeKnown = !page.route().isBlank();
                result.add(new BoundSpaBehaviorAssertion(assertion, "", routeKnown,
                        routeKnown ? "" : "Target route is not confirmed."));
                if (!routeKnown) review.add("Target route is not confirmed for assertion '" + assertion.target() + "'.");
                continue;
            }
            CandidateLocatorEvidence locator = findLocator(components, assertion.target(), evidence);
            boolean verifiable = locator != null;
            String reason = verifiable ? "" : "No confirmed locator binding for assertion target '" + assertion.target() + "'.";
            result.add(new BoundSpaBehaviorAssertion(assertion, verifiable ? locator.locatorId() : "", verifiable, reason));
            if (!verifiable) review.add(reason);
        }
        return List.copyOf(result);
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

    private Map<String, String> resolveData(Map<String, String> rawValues, List<String> review) {
        Map<String, String> result = new LinkedHashMap<>();
        String dataset = rawValues.getOrDefault("dataset", "");
        Map<String, String> datasetValues = Map.of();
        String datasetUnavailable = "";
        if (!dataset.isBlank()) {
            try {
                datasetValues = new PropertiesTestDataProvider().scenarioData(dataset).values();
            } catch (RuntimeException exception) {
                datasetUnavailable = concise(exception);
            }
        }
        for (Map.Entry<String, String> entry : rawValues.entrySet()) {
            String resolved = resolveVariables(entry.getValue(), datasetValues, review);
            if (!resolved.isBlank()) result.put(entry.getKey(), resolved);
        }
        boolean hasDataPlaceholder = rawValues.entrySet().stream()
                .filter(entry -> !"dataset".equalsIgnoreCase(entry.getKey()))
                .anyMatch(entry -> VARIABLE.matcher(entry.getValue()).find());
        if (!datasetUnavailable.isBlank() && hasDataPlaceholder
                && rawValues.entrySet().stream().filter(entry -> !"dataset".equalsIgnoreCase(entry.getKey()))
                .anyMatch(entry -> !result.containsKey(entry.getKey()))) {
            review.add("Scenario dataset '" + dataset + "' is unavailable and required values were not supplied through ENV/system properties: " + datasetUnavailable);
        }
        return Map.copyOf(result);
    }

    private String resolveVariables(String value, Map<String, String> datasetValues, List<String> review) {
        Matcher matcher = VARIABLE.matcher(value == null ? "" : value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String replacement = Optional.ofNullable(System.getenv(key))
                    .orElse(Optional.ofNullable(System.getProperty(key)).orElse(datasetValues.get(key)));
            if (replacement == null || replacement.isBlank()) {
                review.add("Missing data value for '" + key + "'.");
                return "";
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString().trim();
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

    private boolean capabilityComponentMatch(String capability, List<SemanticComponentInventory> components) {
        Set<ComponentType> types = requestedComponentTypes("", capability);
        return components.stream().anyMatch(component -> types.contains(component.type()));
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
        return result;
    }

    private boolean routeAssertion(String type) {
        String normalized = normalize(type);
        return normalized.equals("routechanged") || normalized.equals("urlcontains") || normalized.equals("routeequals")
                || normalized.equals("authenticatedareavisible");
    }

    private boolean requiresExecutableAction(StructuredBehaviorContract contract) {
        return contract.actions().stream().anyMatch(action -> !normalize(action).startsWith("inspect"));
    }

    private String contextValue(String context, String key) {
        String value = context == null ? "" : context;
        Pattern linePattern = Pattern.compile("(?im)^\\s*\\*?\\s*`?" + Pattern.quote(key) + "`?\\s*:\\s*`?([^\\n`]+)");
        Matcher lineMatcher = linePattern.matcher(value);
        if (lineMatcher.find()) {
            return lineMatcher.group(1).trim();
        }
        Pattern inlinePattern = Pattern.compile("(?i)(?:^|\\s|`)" + Pattern.quote(key)
                + "\\s*:\\s*`?([^`;\\n]+)");
        Matcher inlineMatcher = inlinePattern.matcher(value);
        return inlineMatcher.find() ? inlineMatcher.group(1).trim() : "";
    }

    private String explicitRoute(String value) {
        String route = value == null ? "" : value.trim();
        return route.startsWith("/") ? route : "";
    }

    private String semanticTarget(String value) {
        String normalized = value == null ? "" : value
                .replaceAll("(?i)\\b(discovery|confirmed|page|route|target)\\b", " ")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim();
        return normalized;
    }

    private boolean pageMatches(SpaPageInventory page, String targetPage) {
        String target = normalize(targetPage);
        if (target.isBlank()) {
            return false;
        }
        String evidence = normalize(page.pageName() + " " + page.pageId() + " " + page.route());
        return evidence.contains(target) || target.contains(normalize(page.pageName()))
                || expectedTokens(targetPage).stream().allMatch(evidence::contains);
    }

    private boolean routeMatches(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isBlank() && !normalizedRight.isBlank()
                && (normalizedLeft.equals(normalizedRight)
                || normalizedLeft.endsWith(normalizedRight)
                || normalizedRight.endsWith(normalizedLeft));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String concise(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.replaceAll("\\s+", " ").trim();
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
