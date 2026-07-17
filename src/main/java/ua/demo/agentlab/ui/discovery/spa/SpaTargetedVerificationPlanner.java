package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Selects only requirement-owned SPA evidence and validates it against current browser counts. */
public class SpaTargetedVerificationPlanner {

    public SpaTargetedVerificationResult verify(
            SpaInventoryBundle inventory,
            CanonicalTestCaseBundle testCases,
            SpaInventoryConfig config
    ) {
        return verify(inventory, testCases, List.of(), config);
    }

    public SpaTargetedVerificationResult verify(
            SpaInventoryBundle inventory,
            CanonicalTestCaseBundle testCases,
            List<StructuredBehaviorContract> structuredContracts,
            SpaInventoryConfig config
    ) {
        return verify(inventory, testCases, structuredContracts, null, config);
    }

    public SpaTargetedVerificationResult verify(
            SpaInventoryBundle inventory,
            CanonicalTestCaseBundle testCases,
            List<StructuredBehaviorContract> structuredContracts,
            SourceStateBindingBundle sourceBindings,
            SpaInventoryConfig config
    ) {
        if (inventory == null || inventory.pages().isEmpty()) {
            return SpaTargetedVerificationResult.empty(null, "spa-targeted-verification:no-inventory");
        }
        List<CanonicalTestCase> canonicalCases = testCases == null ? List.of() : testCases.testCases();
        List<StructuredBehaviorContract> contracts = structuredContracts == null ? List.of() : structuredContracts;
        if (canonicalCases.isEmpty() && contracts.isEmpty()) {
            return SpaTargetedVerificationResult.empty(inventory.pages().get(0).runMetadata(),
                    "spa-targeted-verification:no-requirement-scope");
        }
        if (config == null || !config.targetedVerificationEnabled()) {
            return SpaTargetedVerificationResult.empty(inventory.pages().get(0).runMetadata(),
                    "spa-targeted-verification:disabled");
        }
        if (sourceBindings != null) {
            return verifySourceBindings(inventory, sourceBindings, config);
        }

        Map<String, Set<String>> requirementIdsByPage = requirementsByPage(inventory.pages(), canonicalCases, contracts);
        List<TargetedLocatorVerification> locators = new ArrayList<>();
        List<TargetedActionVerification> actions = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        for (SpaPageInventory page : inventory.pages()) {
            Set<String> requirementIds = requirementIdsByPage.getOrDefault(page.pageId(), Set.of());
            if (requirementIds.isEmpty()) {
                continue;
            }
            Set<UiOperationKind> operations = operationsFor(page, canonicalCases, contracts);
            Set<ComponentType> allowedComponents = componentsFor(operations);
            for (SemanticComponentInventory component : page.components()) {
                if (!componentRelevant(component, allowedComponents, operations)) {
                    continue;
                }
                Map<String, TargetedLocatorVerification> verificationByLocator = new LinkedHashMap<>();
                for (CandidateLocatorEvidence locator : component.locators()) {
                    TargetedLocatorVerification verification = verifyLocator(page, component, locator, requirementIds, config);
                    verificationByLocator.put(locator.locatorId(), verification);
                    locators.add(verification);
                    if (!verification.verified()) {
                        excluded.add("locator=" + locator.locatorId() + " reason=" + verification.reason());
                    }
                }
                for (CandidateActionEvidence action : component.actions()) {
                    if (!actionRelevant(action, operations)) {
                        continue;
                    }
                    TargetedActionVerification verification = verifyAction(page, component, action,
                            verificationByLocator, requirementIds, config);
                    actions.add(verification);
                    if (!verification.verified()) {
                        excluded.add("action=" + action.actionId() + " reason=" + verification.reason());
                    }
                }
            }
        }
        return new SpaTargetedVerificationResult(
                SpaTargetedVerificationResult.SCHEMA_VERSION,
                inventory.pages().get(0).runMetadata(),
                distinctLocators(locators),
                distinctActions(actions),
                List.copyOf(excluded),
                List.of(
                        "spa-targeted-verification:requirement-scope",
                        "target-pages=" + requirementIdsByPage.size(),
                        "mode=" + inventory.mode().name().toLowerCase(Locale.ROOT),
                        "browser-counts-required"
                )
        );
    }

    private SpaTargetedVerificationResult verifySourceBindings(
            SpaInventoryBundle inventory,
            SourceStateBindingBundle sourceBindings,
            SpaInventoryConfig config
    ) {
        List<TargetedLocatorVerification> locators = new ArrayList<>();
        List<TargetedActionVerification> actions = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        Map<String, SpaPageInventory> pages = inventory.pages().stream().collect(java.util.stream.Collectors.toMap(
                SpaPageInventory::pageId, page -> page, (left, right) -> left, LinkedHashMap::new));
        for (SourceStateBinding binding : sourceBindings.bindings()) {
            if (!binding.liveVerificationEligible()) {
                excluded.add("requirement=" + binding.requirementId() + " reason=" + String.join("; ", binding.reviewReasons()));
                continue;
            }
            SpaPageInventory page = pages.get(binding.sourcePageId());
            if (page == null) {
                excluded.add("requirement=" + binding.requirementId() + " reason=source page is absent from inventory");
                continue;
            }
            Set<String> selectedLocators = Set.copyOf(binding.candidateLocatorIds());
            Set<String> selectedActions = Set.copyOf(binding.candidateActionIds());
            Map<String, TargetedLocatorVerification> byLocator = new LinkedHashMap<>();
            for (SemanticComponentInventory component : page.components()) {
                if (!binding.componentIds().contains(component.componentId())) continue;
                for (CandidateLocatorEvidence locator : component.locators()) {
                    if (!selectedLocators.contains(locator.locatorId())) continue;
                    TargetedLocatorVerification admitted = admitForLive(page, component, locator, binding.requirementId(), config);
                    byLocator.put(locator.locatorId(), admitted);
                    locators.add(admitted);
                    if (!admitted.verified()) excluded.add("locator=" + locator.locatorId() + " reason=" + admitted.reason());
                }
                for (CandidateActionEvidence action : component.actions()) {
                    if (!selectedActions.contains(action.actionId())) continue;
                    // requiredLocatorIds are alternative selectors for the same semantic element.
                    // One browser-verifiable selector is sufficient to admit the action.
                    boolean locatorAdmitted = !action.requiredLocatorIds().isEmpty() && action.requiredLocatorIds().stream()
                            .map(byLocator::get).anyMatch(item -> item != null && item.verified());
                    boolean admitted = action.confidence() >= config.minLiveVerificationScore() && locatorAdmitted;
                    TargetedActionVerification result = new TargetedActionVerification(page.pageId(), page.route(),
                            page.pageFingerprintHash(), component.componentId(), action.actionId(), action.intent(),
                            action.targetElementId(), action.confidence(), admitted,
                            admitted ? "requirement-relevant candidate admitted to live browser verification"
                                    : "candidate action failed live-verification admission policy",
                            List.of(binding.requirementId()));
                    actions.add(result);
                    if (!admitted) excluded.add("action=" + action.actionId() + " reason=" + result.reason());
                }
            }
        }
        return new SpaTargetedVerificationResult(SpaTargetedVerificationResult.SCHEMA_VERSION,
                sourceBindings.runMetadata(), distinctLocators(locators), distinctActions(actions), List.copyOf(excluded),
                List.of("spa-targeted-verification:source-state-bindings",
                        "candidate-admission-threshold=" + config.minLiveVerificationScore(),
                        "promotion-threshold=" + config.minConfirmedScore()));
    }

    private TargetedLocatorVerification admitForLive(SpaPageInventory page, SemanticComponentInventory component,
                                                      CandidateLocatorEvidence locator, String requirementId,
                                                      SpaInventoryConfig config) {
        boolean unique = locator.globalMatchCount() == 1 || locator.componentMatchCount() == 1;
        boolean safe = locator.sameOrigin() && locator.risks().stream().noneMatch(risk -> {
            String value = normalize(risk);
            return value.contains("external") || value.contains("absolute") || value.contains("hidden") || value.contains("missing");
        });
        boolean admitted = locator.qualityScore() >= config.minLiveVerificationScore() && unique && safe;
        return new TargetedLocatorVerification(page.pageId(), page.route(), page.pageFingerprintHash(), component.componentId(),
                locator.locatorId(), locator.elementId(), locator.strategy(), locator.value(), locator.qualityScore(), admitted,
                admitted ? "requirement-relevant candidate admitted to live browser verification"
                        : "candidate locator failed live-verification admission policy", List.of(requirementId));
    }

    private Map<String, Set<String>> requirementsByPage(
            List<SpaPageInventory> pages,
            List<CanonicalTestCase> testCases,
            List<StructuredBehaviorContract> contracts
    ) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (SpaPageInventory page : pages) {
            for (CanonicalTestCase testCase : testCases) {
                if (belongsToPage(page, testCase)) {
                    result.computeIfAbsent(page.pageId(), ignored -> new LinkedHashSet<>()).addAll(testCase.requirementRefs());
                }
            }
            for (StructuredBehaviorContract contract : contracts) {
                if (belongsToPage(page, contract)) {
                    result.computeIfAbsent(page.pageId(), ignored -> new LinkedHashSet<>()).add(contract.requirementId());
                }
            }
        }
        return result;
    }

    private Set<UiOperationKind> operationsFor(
            SpaPageInventory page,
            List<CanonicalTestCase> testCases,
            List<StructuredBehaviorContract> contracts
    ) {
        Set<UiOperationKind> operations = EnumSet.noneOf(UiOperationKind.class);
        for (CanonicalTestCase testCase : testCases) {
            if (!belongsToPage(page, testCase)) {
                continue;
            }
            testCase.operationIntents().stream()
                    .filter(intent -> intentBelongsToPage(intent, page))
                    .forEach(intent -> operations.add(intent.kind()));
        }
        for (StructuredBehaviorContract contract : contracts) {
            if (!belongsToPage(page, contract)) continue;
            operations.addAll(operationsFor(contract));
        }
        return operations;
    }

    private Set<UiOperationKind> operationsFor(StructuredBehaviorContract contract) {
        String capability = normalize(contract.capability()).replace('-', '_');
        return switch (capability) {
            case "module_navigation" -> EnumSet.of(UiOperationKind.OPEN_PAGE);
            case "filter" -> EnumSet.of(UiOperationKind.FILTER);
            case "search" -> EnumSet.of(UiOperationKind.SEARCH);
            case "record_list" -> EnumSet.of(UiOperationKind.INSPECT_COLLECTION);
            case "open_modal" -> EnumSet.of(UiOperationKind.OPEN_MODAL);
            default -> EnumSet.noneOf(UiOperationKind.class);
        };
    }

    private boolean intentBelongsToPage(ua.demo.agentlab.ui.contract.UiOperationIntent intent, SpaPageInventory page) {
        if (intent == null || page == null) {
            return false;
        }
        if (intent.target() == null || intent.target().isBlank()) {
            return false;
        }
        return matchesPageName(page.pageName(), intent.target()) || matches(page.route(), intent.target());
    }

    private boolean belongsToPage(SpaPageInventory page, CanonicalTestCase testCase) {
        if (page == null || testCase == null) {
            return false;
        }
        String route = normalize(page.route());
        return matches(route, testCase.route())
                || matches(route, testCase.sourceRoute())
                || matchesPageName(page.pageName(), testCase.pageName())
                || matchesPageName(page.pageName(), testCase.sourcePageName())
                || testCase.targetPages().stream().anyMatch(name -> matchesPageName(page.pageName(), name));
    }

    private boolean belongsToPage(SpaPageInventory page, StructuredBehaviorContract contract) {
        if (page == null || contract == null) return false;
        boolean moduleNavigation = "module_navigation".equals(normalize(contract.capability()).replace('-', '_'));
        if (moduleNavigation) {
            String sourceRoute = contextValue(contract.targetContext(), "sourceRoute");
            if (sourceRoute.startsWith("/")) {
                return matches(page.route(), sourceRoute);
            }
            String sourcePage = semanticTarget(contextValue(contract.targetContext(), "sourcePage"));
            if (!sourcePage.isBlank()) {
                String evidence = normalize(String.join(" ", page.pageName(), page.route(), page.pageId()));
                Set<String> tokens = meaningfulTokens(sourcePage);
                return !tokens.isEmpty() && tokens.stream().allMatch(evidence::contains);
            }
        }
        String explicitRoute = contextValue(contract.targetContext(), "targetRoute");
        if (explicitRoute.startsWith("/")) {
            return matches(page.route(), explicitRoute);
        }
        String targetPage = semanticTarget(contextValue(contract.targetContext(), "targetPage"));
        if (!targetPage.isBlank()) {
            String evidence = normalize(String.join(" ", page.pageName(), page.route(), page.pageId()));
            Set<String> tokens = meaningfulTokens(targetPage);
            return !tokens.isEmpty() && tokens.stream().allMatch(evidence::contains);
        }
        String capability = contextValue(contract.targetContext(), "pageCapability");
        if (!capability.isBlank()) {
            return normalize(page.capability()).contains(normalize(capability));
        }
        return false;
    }

    private Set<ComponentType> componentsFor(Set<UiOperationKind> operations) {
        Set<ComponentType> types = EnumSet.noneOf(ComponentType.class);
        for (UiOperationKind operation : operations) {
            switch (operation) {
                case AUTHENTICATE, ENTER_TEXT, SUBMIT_FORM, CREATE_RECORD, EDIT_RECORD, UPLOAD_FILE ->
                        types.add(ComponentType.FORM);
                case LOGOUT -> {
                    types.add(ComponentType.USER_MENU);
                    types.add(ComponentType.HEADER);
                }
                case SEARCH -> types.add(ComponentType.SEARCH);
                case FILTER -> types.add(ComponentType.FILTER_PANEL);
                case OPEN_RECORD, INSPECT_COLLECTION, SORT_COLLECTION, PAGINATE, SORT -> {
                    types.add(ComponentType.RESULTS_COLLECTION);
                    types.add(ComponentType.TABLE);
                }
                case OPEN_MODAL, CONFIRM_ACTION -> types.add(ComponentType.MODAL);
                case OPEN_PAGE, VERIFY_PAGE_ACCESS, INSPECT_PAGE_CONTENT, VERIFY_PUBLIC_ACCESS,
                        OPEN_DETAILS -> {
                    types.add(ComponentType.NAVIGATION);
                    types.add(ComponentType.CONTENT);
                }
                default -> { }
            }
        }
        return types;
    }

    private boolean componentRelevant(
            SemanticComponentInventory component,
            Set<ComponentType> allowedComponents,
            Set<UiOperationKind> operations
    ) {
        if (allowedComponents.isEmpty()) {
            return false;
        }
        if (allowedComponents.contains(component.type())) {
            return true;
        }
        return operations.contains(UiOperationKind.LOGOUT) && component.type() == ComponentType.NAVIGATION;
    }

    private Set<String> meaningfulTokens(String text) {
        Set<String> excluded = Set.of("target", "source", "route", "page", "component", "capability", "discovery",
                "confirmed", "authentication", "authenticated", "application", "navigation", "visible", "open");
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalize(text).split("[^a-z0-9]+")) {
            if (token.length() >= 4 && !excluded.contains(token)) tokens.add(token);
        }
        return tokens;
    }

    private TargetedLocatorVerification verifyLocator(
            SpaPageInventory page,
            SemanticComponentInventory component,
            CandidateLocatorEvidence locator,
            Set<String> requirementIds,
            SpaInventoryConfig config
    ) {
        boolean browserUnique = locator.globalMatchCount() == 1 || locator.componentMatchCount() == 1;
        boolean riskFree = locator.risks().stream().noneMatch(this::blockingRisk);
        boolean verified = locator.qualityScore() >= config.minConfirmedScore()
                && locator.sameOrigin()
                && locator.stableAcrossRuns()
                && browserUnique
                && riskFree;
        String reason = verified ? "browser-validated candidate" : failureReason(locator, browserUnique, riskFree, config);
        return new TargetedLocatorVerification(
                page.pageId(), page.route(), page.pageFingerprintHash(), component.componentId(), locator.locatorId(),
                locator.elementId(), locator.strategy(), locator.value(), locator.qualityScore(), verified, reason,
                List.copyOf(requirementIds)
        );
    }

    private TargetedActionVerification verifyAction(
            SpaPageInventory page,
            SemanticComponentInventory component,
            CandidateActionEvidence action,
            Map<String, TargetedLocatorVerification> verifications,
            Set<String> requirementIds,
            SpaInventoryConfig config
    ) {
        boolean hasLocators = !action.requiredLocatorIds().isEmpty();
        boolean locatorsVerified = hasLocators && action.requiredLocatorIds().stream()
                .map(verifications::get)
                .anyMatch(verification -> verification != null && verification.verified());
        boolean verified = action.confidence() >= config.minConfirmedScore() && locatorsVerified;
        String reason = verified ? "at least one alternative locator browser-validated"
                : hasLocators ? "no alternative locator is confirmed" : "action has no component-owned locator evidence";
        return new TargetedActionVerification(
                page.pageId(), page.route(), page.pageFingerprintHash(), component.componentId(), action.actionId(),
                action.intent(), action.targetElementId(), action.confidence(), verified, reason,
                List.copyOf(requirementIds)
        );
    }

    private boolean actionRelevant(CandidateActionEvidence action, Set<UiOperationKind> operations) {
        String intent = action == null || action.intent() == null ? "" : action.intent().trim().toUpperCase(Locale.ROOT);
        for (UiOperationKind operation : operations) {
            if (matchesIntent(operation, intent)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesIntent(UiOperationKind operation, String intent) {
        return switch (operation) {
            case AUTHENTICATE -> Set.of("TYPE", "CLEAR", "CLICK", "SUBMIT_FORM").contains(intent);
            case ENTER_TEXT -> Set.of("TYPE", "CLEAR").contains(intent);
            case SUBMIT_FORM -> Set.of("SUBMIT_FORM", "CLICK").contains(intent);
            case LOGOUT -> Set.of("OPEN_MENU", "LOGOUT", "CLICK").contains(intent);
            case SEARCH -> Set.of("SEARCH", "TYPE", "CLICK").contains(intent);
            case FILTER -> Set.of("FILTER", "SELECT", "CLICK").contains(intent);
            case OPEN_RECORD, OPEN_DETAILS -> Set.of("OPEN_RECORD", "CLICK").contains(intent);
            case CREATE_RECORD -> Set.of("CREATE_RECORD", "CLICK").contains(intent);
            case EDIT_RECORD -> Set.of("EDIT_RECORD", "CLICK").contains(intent);
            case DELETE_RECORD -> Set.of("DELETE_RECORD", "CLICK").contains(intent);
            case OPEN_MODAL -> Set.of("OPEN_MODAL", "CLICK").contains(intent);
            case CONFIRM_ACTION -> Set.of("CONFIRM_ACTION", "CLICK").contains(intent);
            case SORT_COLLECTION, SORT -> Set.of("SORT_COLLECTION", "CLICK").contains(intent);
            case PAGINATE -> Set.of("PAGINATE", "CLICK").contains(intent);
            case OPEN_PAGE, VERIFY_PAGE_ACCESS, OPEN_TARGET_CONTAINER, OPEN_DESTINATION_CONTAINER ->
                    Set.of("CLICK", "OPEN_RECORD").contains(intent);
            default -> false;
        };
    }

    private List<TargetedLocatorVerification> distinctLocators(List<TargetedLocatorVerification> values) {
        Map<String, TargetedLocatorVerification> deduped = new LinkedHashMap<>();
        values.stream().sorted(Comparator.comparing(TargetedLocatorVerification::locatorId))
                .forEach(value -> deduped.putIfAbsent(value.locatorId(), value));
        return List.copyOf(deduped.values());
    }

    private List<TargetedActionVerification> distinctActions(List<TargetedActionVerification> values) {
        Map<String, TargetedActionVerification> deduped = new LinkedHashMap<>();
        values.stream().sorted(Comparator.comparing(TargetedActionVerification::actionId))
                .forEach(value -> deduped.putIfAbsent(value.actionId(), value));
        return List.copyOf(deduped.values());
    }

    private boolean blockingRisk(String risk) {
        String normalized = normalize(risk);
        return normalized.contains("external") || normalized.contains("unstable") || normalized.contains("missing")
                || normalized.contains("absolute") || normalized.contains("dynamic") || normalized.contains("hidden");
    }

    private String failureReason(
            CandidateLocatorEvidence locator,
            boolean browserUnique,
            boolean riskFree,
            SpaInventoryConfig config
    ) {
        if (locator.qualityScore() < config.minConfirmedScore()) {
            return "quality score below configured threshold";
        }
        if (!locator.stableAcrossRuns()) {
            return "locator is not stable across discovery runs";
        }
        if (!locator.sameOrigin()) {
            return "locator is not same-origin";
        }
        if (!browserUnique) {
            return "locator is not unique globally or within component";
        }
        return riskFree ? "unknown verification failure" : "locator has blocking SPA risk";
    }

    private boolean matches(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isBlank() && !normalizedRight.isBlank()
                && (normalizedLeft.equals(normalizedRight)
                || normalizedLeft.endsWith(normalizedRight)
                || normalizedRight.endsWith(normalizedLeft));
    }

    private boolean matchesPageName(String left, String right) {
        String normalizedLeft = normalize(left).replaceAll("[^a-z0-9]", "");
        String normalizedRight = normalize(right).replaceAll("[^a-z0-9]", "");
        return !normalizedLeft.isBlank() && !normalizedRight.isBlank()
                && (normalizedLeft.equals(normalizedRight)
                || normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String contextValue(String context, String key) {
        String boundary = "pageCapability|componentCapability|sourceRoute|targetRoute|sourcePage|targetPage";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)(?:^|\\s|`)"
                        + java.util.regex.Pattern.quote(key)
                        + "\\s*:\\s*`?(.+?)(?=\\s*;?\\s+(?:" + boundary + ")\\s*:|$)")
                .matcher(context == null ? "" : context);
        return matcher.find() ? matcher.group(1).replace("`", "").replaceAll("[;\\s]+$", "").trim() : "";
    }

    private String semanticTarget(String value) {
        return value == null ? "" : value.replaceAll("(?i)\\b(discovery|confirmed|page|route|target)\\b", " ")
                .replaceAll("[^A-Za-z0-9]+", " ").trim();
    }
}
