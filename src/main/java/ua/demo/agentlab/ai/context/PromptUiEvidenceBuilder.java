package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.discovery.knowledge.model.ExcludedEvidence;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.component.ComponentBoundaryDetector;
import ua.demo.agentlab.ui.discovery.component.model.ComponentDiscoveryModel;
import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentModel;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentPageModel;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;
import ua.demo.agentlab.ui.discovery.semantic.model.ActionCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.BusinessIntentCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticElementModel;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticPageModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PromptUiEvidenceBuilder {

    private final PromptLocatorSelector locatorSelector;
    private final SemanticActionModelBuilder semanticActionModelBuilder;
    private final ComponentBoundaryDetector componentBoundaryDetector;

    public PromptUiEvidenceBuilder() {
        this(new PromptLocatorSelector(), new SemanticActionModelBuilder(), new ComponentBoundaryDetector());
    }

    PromptUiEvidenceBuilder(PromptLocatorSelector locatorSelector) {
        this(locatorSelector, new SemanticActionModelBuilder(), new ComponentBoundaryDetector());
    }

    PromptUiEvidenceBuilder(PromptLocatorSelector locatorSelector, SemanticActionModelBuilder semanticActionModelBuilder) {
        this(locatorSelector, semanticActionModelBuilder, new ComponentBoundaryDetector());
    }

    PromptUiEvidenceBuilder(
            PromptLocatorSelector locatorSelector,
            SemanticActionModelBuilder semanticActionModelBuilder,
            ComponentBoundaryDetector componentBoundaryDetector
    ) {
        this.locatorSelector = locatorSelector == null ? new PromptLocatorSelector() : locatorSelector;
        this.semanticActionModelBuilder = semanticActionModelBuilder == null
                ? new SemanticActionModelBuilder()
                : semanticActionModelBuilder;
        this.componentBoundaryDetector = componentBoundaryDetector == null
                ? new ComponentBoundaryDetector()
                : componentBoundaryDetector;
    }

    public PromptUiEvidence build(AiContextPackage context) {
        if (context == null || context.mappedUiKnowledge() == null || context.mappedUiKnowledge().pages().isEmpty()) {
            return PromptUiEvidence.empty("prompt-evidence:no-mapped-page-scope");
        }
        MappedPage targetPage = context.mappedUiKnowledge().pages().get(0);
        Set<String> requirementIds = requirementIds(context);
        List<PromptActionEvidence> actions = actionEvidence(context, targetPage);
        List<PromptAssertionEvidence> assertions = assertionEvidence(context, targetPage);
        List<PromptLocatorEvidence> locators = locatorEvidence(context, targetPage);
        List<ExcludedEvidence> excluded = context.mappedUiKnowledgeCurated() == null
                ? List.of()
                : context.mappedUiKnowledgeCurated().excludedEvidence().stream()
                .filter(evidence -> evidence.pageId().isBlank() || evidence.pageId().equals(targetPage.pageId()))
                .limit(20)
                .toList();
        List<String> sourceTrace = new ArrayList<>();
        sourceTrace.add("prompt-evidence:scoped-context");
        sourceTrace.add("prompt-evidence:targetPage=" + targetPage.pageName());
        sourceTrace.add("prompt-evidence:requirementIds=" + requirementIds);
        return new PromptUiEvidence(
                targetPage.pageName(),
                route(targetPage),
                new ArrayList<>(requirementIds),
                actions,
                assertions,
                locators,
                List.of(),
                excluded,
                sourceTrace,
                confidence(context, locators, assertions)
        );
    }

    private Set<String> requirementIds(AiContextPackage context) {
        Set<String> ids = new LinkedHashSet<>();
        if (context.canonicalTestCaseBundle() != null) {
            context.canonicalTestCaseBundle().testCases().forEach(testCase -> ids.addAll(testCase.requirementRefs()));
        }
        context.assertionContracts().forEach(contract -> addIfPresent(ids, contract.requirementId()));
        return ids;
    }

    private List<PromptActionEvidence> actionEvidence(AiContextPackage context, MappedPage targetPage) {
        List<PromptActionEvidence> actions = new ArrayList<>();
        boolean includePageOwnedActions = hasPageOwnedActionScenarios(context, targetPage);
        if (!includePageOwnedActions) {
            return List.of();
        }
        actions.addAll(semanticActionEvidence(context, targetPage));
        if (context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                if (!canonicalTestCaseSourceBelongsToTarget(testCase, targetPage)) {
                    continue;
                }
                testCase.actions().forEach(action -> actions.add(new PromptActionEvidence(
                        action,
                        "canonical-test-action",
                        ownerPage(testCase),
                        testCase.id()
                )));
                testCase.operationIntents().forEach(intent -> actions.add(new PromptActionEvidence(
                        intent.kind() == null ? "" : intent.kind().name(),
                        "operation-intent",
                        ownerPage(testCase),
                        testCase.id()
                )));
            }
        }
        for (MappedAction action : targetPage.actions()) {
            actions.add(new PromptActionEvidence(
                    action.actionName(),
                    action.actionType(),
                    targetPage.pageName(),
                    "mapped-action:" + action.actionId()
            ));
        }
        return deduplicateActions(actions).stream().limit(12).toList();
    }

    private boolean hasPageOwnedActionScenarios(AiContextPackage context, MappedPage targetPage) {
        if (context == null || context.canonicalTestCaseBundle() == null) {
            return isAuthenticationPage(targetPage);
        }
        return context.canonicalTestCaseBundle().testCases().stream()
                .anyMatch(testCase -> canonicalTestCaseSourceBelongsToTarget(testCase, targetPage)
                        && (!testCase.actions().isEmpty() || !testCase.operationIntents().isEmpty()));
    }

    private boolean canonicalTestCaseSourceBelongsToTarget(CanonicalTestCase testCase, MappedPage targetPage) {
        if (testCase == null || targetPage == null) {
            return false;
        }
        return pageNameMatches(testCase.sourcePageName(), targetPage)
                || RouteCanonicalizer.routeEqualsOrSuffix(testCase.sourceRoute(), route(targetPage));
    }

    private List<PromptActionEvidence> semanticActionEvidence(AiContextPackage context, MappedPage targetPage) {
        SemanticPageModel semanticPage = semanticActionModelBuilder.buildForTarget(
                context.pageModelBundle(),
                context.mappedUiKnowledge(),
                targetPage
        );
        if (semanticPage == null) {
            return List.of();
        }
        List<PromptActionEvidence> actions = new ArrayList<>();
        for (BusinessIntentCandidate intent : semanticPage.pageBusinessIntentCandidates()) {
            if (intent.confidence() >= 0.70d) {
                actions.add(new PromptActionEvidence(
                        intent.intent(),
                        "semantic-business-intent",
                        targetPage.pageName(),
                        "semantic-page:" + semanticPage.pageId()
                ));
            }
        }
        Map<String, SemanticElementModel> elementsById = semanticPage.elements().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SemanticElementModel::elementId,
                        element -> element,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (ActionCandidate action : semanticPage.pageActionCandidates()) {
            if (action.confidence() >= 0.76d) {
                SemanticElementModel element = elementsById.get(action.targetElementId());
                actions.add(new PromptActionEvidence(
                        element == null || element.name().isBlank()
                                ? action.action()
                                : element.name() + ":" + action.action(),
                        "semantic-action",
                        targetPage.pageName(),
                        "semantic-action:" + action.targetElementId()
                ));
            }
        }
        for (SemanticElementModel element : semanticPage.elements()) {
            for (BusinessIntentCandidate intent : element.businessIntentCandidates()) {
                if (intent.confidence() >= 0.78d) {
                    actions.add(new PromptActionEvidence(
                            element.name() + ":" + intent.intent(),
                            "semantic-element-intent",
                            targetPage.pageName(),
                            "semantic-element:" + element.elementId()
                    ));
                }
            }
        }
        return actions;
    }

    private List<PromptAssertionEvidence> assertionEvidence(AiContextPackage context, MappedPage targetPage) {
        List<PromptAssertionEvidence> assertions = new ArrayList<>();
        for (AssertionContract contract : context.assertionContracts()) {
            if (assertionContractBelongsToTarget(contract, targetPage)) {
                assertions.add(new PromptAssertionEvidence(
                        contract.type().name(),
                        contract.expectedValue(),
                        contract.ownerPage(),
                        contract.sourceLine().isBlank() ? contract.testCaseId() : contract.sourceLine(),
                        contract.confidence()
                ));
            }
        }
        if (context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                if (!canonicalTestCaseBelongsToTarget(testCase, targetPage)) {
                    continue;
                }
                testCase.assertions().forEach(assertion -> {
                    if (assertionTextFitsTarget("CANONICAL_ASSERTION", assertion, targetPage)) {
                        assertions.add(new PromptAssertionEvidence(
                                "CANONICAL_ASSERTION",
                                assertion,
                                testCase.pageName(),
                                testCase.id(),
                                0.70d
                        ));
                    }
                });
                testCase.assertionIntents().forEach(intent -> {
                    String type = intent.kind() == null ? "" : intent.kind().name();
                    if (assertionTextFitsTarget(type, intent.expectedValue(), targetPage)) {
                        assertions.add(new PromptAssertionEvidence(
                                type,
                                intent.expectedValue(),
                                testCase.pageName(),
                                testCase.id(),
                                0.75d
                        ));
                    }
                });
            }
        }
        targetPage.assertionHints().forEach(hint -> assertions.add(new PromptAssertionEvidence(
                hint.hintType(),
                hint.target().isBlank() ? hint.description() : hint.target(),
                targetPage.pageName(),
                "mapped-assertion-hint",
                hint.confidenceScore()
        )));
        return assertions.stream()
                .filter(assertion -> !assertion.type().isBlank() || !assertion.expectedValue().isBlank())
                .limit(16)
                .toList();
    }

    private boolean assertionContractBelongsToTarget(AssertionContract contract, MappedPage targetPage) {
        if (contract == null) {
            return false;
        }
        boolean ownerMatches = pageNameMatches(contract.ownerPage(), targetPage)
                || RouteCanonicalizer.routeEqualsOrSuffix(contract.route(), route(targetPage));
        return ownerMatches && assertionTextFitsTarget(contract.type().name(), contract.expectedValue(), targetPage);
    }

    private boolean assertionTextFitsTarget(String type, String expectedValue, MappedPage targetPage) {
        String expected = expectedValue == null ? "" : expectedValue.trim();
        String normalized = expected.toLowerCase(Locale.ROOT);
        if ("URL_CONTAINS".equalsIgnoreCase(type) || "ROUTE_EQUALS".equalsIgnoreCase(type)) {
            return RouteCanonicalizer.routeEqualsOrSuffix(expected, route(targetPage));
        }
        if (containsAny(normalized, "login form", "username", "password input", "login button")
                && !isLoginPage(targetPage)) {
            return false;
        }
        if (containsAny(normalized, "registration", "forgot password", "password recovery")
                && !isLoginPage(targetPage)) {
            return false;
        }
        if (containsAny(normalized, "/profile", "account/profile")
                && !RouteCanonicalizer.routeEqualsOrSuffix("/profile", route(targetPage))) {
            return false;
        }
        return true;
    }

    private boolean canonicalTestCaseBelongsToTarget(CanonicalTestCase testCase, MappedPage targetPage) {
        if (testCase == null || targetPage == null) {
            return false;
        }
        if (pageNameMatches(testCase.pageName(), targetPage)
                || pageNameMatches(testCase.sourcePageName(), targetPage)
                || RouteCanonicalizer.routeEqualsOrSuffix(testCase.route(), route(targetPage))
                || RouteCanonicalizer.routeEqualsOrSuffix(testCase.sourceRoute(), route(targetPage))) {
            return true;
        }
        for (String page : testCase.targetPages()) {
            if (pageNameMatches(page, targetPage) || RouteCanonicalizer.routeEqualsOrSuffix(page, route(targetPage))) {
                return true;
            }
        }
        return false;
    }

    private boolean pageNameMatches(String ownerPage, MappedPage targetPage) {
        String owner = fieldHint(ownerPage);
        String target = fieldHint(targetPage.pageName());
        return !owner.isBlank() && owner.equalsIgnoreCase(target);
    }

    private boolean isLoginPage(MappedPage targetPage) {
        String pageName = targetPage.pageName().toLowerCase(Locale.ROOT);
        String route = route(targetPage).toLowerCase(Locale.ROOT);
        return pageName.contains("login") || route.contains("login") || route.contains("auth/login");
    }

    private boolean isAuthenticationPage(MappedPage targetPage) {
        if (targetPage == null) {
            return false;
        }
        String pageName = targetPage.pageName().toLowerCase(Locale.ROOT);
        String pageType = targetPage.pageType().toLowerCase(Locale.ROOT);
        String route = route(targetPage).toLowerCase(Locale.ROOT);
        return pageName.contains("login")
                || pageType.equals("authentication")
                || route.contains("login")
                || route.contains("auth/login");
    }

    private List<PromptLocatorEvidence> locatorEvidence(AiContextPackage context, MappedPage targetPage) {
        List<PromptLocatorEvidence> locators = new ArrayList<>();
        locators.addAll(componentLocatorEvidence(context, targetPage));
        targetPage.forms().forEach(form -> form.fields().forEach(field ->
                locatorSelector.select(field.locatorCandidates(), field.fieldName(), field.fieldType(), field.label()).ifPresent(locator ->
                        locators.add(toLocatorEvidence(field.fieldName(), field.fieldType(), field.label(), locator)))));
        for (MappedElement element : targetPage.elements()) {
            locatorSelector.select(element.locatorCandidates(), element.semanticName(), element.role(), element.text()).ifPresent(locator ->
                    locators.add(toLocatorEvidence(element.semanticName(), element.role(), element.text(), locator)));
        }
        if (locators.isEmpty()) {
            locators.addAll(pageModelLocatorEvidence(context, targetPage));
        }
        return deduplicateLocators(locators).stream().limit(16).toList();
    }

    private List<PromptLocatorEvidence> componentLocatorEvidence(AiContextPackage context, MappedPage targetPage) {
        if (context.pageModelBundle() == null || context.pageModelBundle().pages().isEmpty()) {
            return List.of();
        }
        ComponentDiscoveryModel componentModel = componentBoundaryDetector.detect(context.pageModelBundle());
        PageModel pageModel = context.pageModelBundle().pages().stream()
                .filter(page -> pageModelMatchesTarget(page, targetPage))
                .findFirst()
                .orElse(null);
        if (pageModel == null) {
            return List.of();
        }
        Map<String, PageElementModel> elementsById = pageModel.elements().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PageElementModel::elementId,
                        element -> element,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return componentModel.pages().stream()
                .filter(page -> componentPageMatchesTarget(page, pageModel, targetPage))
                .flatMap(page -> page.components().stream())
                .flatMap(component -> bestComponentLocators(component, elementsById, targetPage).stream())
                .limit(16)
                .toList();
    }

    private boolean componentPageMatchesTarget(
            SemanticComponentPageModel componentPage,
            PageModel pageModel,
            MappedPage targetPage
    ) {
        if (componentPage.pageId().equalsIgnoreCase(pageModel.pageId())) {
            return true;
        }
        String targetRoute = route(targetPage);
        return RouteCanonicalizer.routeEqualsOrSuffix(componentPage.route(), targetRoute)
                || RouteCanonicalizer.routeEqualsOrSuffix(componentPage.route(), targetPage.url());
    }

    private List<PromptLocatorEvidence> bestComponentLocators(
            SemanticComponentModel component,
            Map<String, PageElementModel> elementsById,
            MappedPage targetPage
    ) {
        Map<String, PromptLocatorEvidence> bestByElement = new LinkedHashMap<>();
        for (ScopedLocatorCandidate candidate : component.locators()) {
            PageElementModel element = elementsById.get(candidate.elementId());
            if (element == null || !componentPromptSafeLocator(component, candidate, element, targetPage)) {
                continue;
            }
            PromptLocatorEvidence evidence = toComponentLocatorEvidence(component, candidate, element);
            PromptLocatorEvidence existing = bestByElement.get(candidate.elementId());
            if (existing == null || evidence.stabilityScore() > existing.stabilityScore()) {
                bestByElement.put(candidate.elementId(), evidence);
            }
        }
        return bestByElement.values().stream()
                .sorted(Comparator.comparingDouble(PromptLocatorEvidence::stabilityScore).reversed())
                .limit(8)
                .toList();
    }

    private boolean componentPromptSafeLocator(
            SemanticComponentModel component,
            ScopedLocatorCandidate candidate,
            PageElementModel element,
            MappedPage targetPage
    ) {
        if (candidate == null || candidate.value().isBlank() || !element.visible()) {
            return false;
        }
        if (!candidate.uniqueWithinComponent()) {
            return false;
        }
        if (candidate.finalScore() < 0.72d) {
            return false;
        }
        if (candidate.risks().stream().anyMatch(this::componentForbiddenRisk)) {
            return false;
        }
        String evidence = String.join(" ",
                element.elementId(),
                element.semanticType(),
                element.technicalType(),
                element.tag(),
                element.inputType(),
                element.name(),
                element.id(),
                element.placeholder(),
                element.ariaLabel(),
                element.role(),
                element.href(),
                element.text(),
                candidate.value()
        ).toLowerCase(Locale.ROOT);
        if (containsAny(evidence, "csrf", "xsrf", "token", "_token", "authenticity_token")) {
            return false;
        }
        PageLocatorModel locator = new PageLocatorModel(
                candidate.strategy(),
                candidate.value(),
                candidate.finalScore(),
                "component scoped locator",
                candidate.uniqueOnPage(),
                1,
                1,
                true,
                candidate.globalMatchCount(),
                candidate.scopedMatchCount(),
                component.name()
        );
        return promptSafePageModelLocator(element, locator, targetPage)
                || candidate.uniqueWithinComponent() && !externalHref(element.href(), targetPage)
                && !externalLocatorValue(candidate.value(), targetPage);
    }

    private boolean componentForbiddenRisk(String risk) {
        String normalized = risk == null ? "" : risk.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("dynamic-css-hash")
                || normalized.equals("nth-child-selector")
                || normalized.equals("absolute-dom-path")
                || normalized.equals("deep-dom-chain")
                || normalized.equals("framework-generated-class")
                || normalized.equals("security-token-field")
                || normalized.equals("hidden-or-invisible-element")
                || normalized.equals("not-component-unique");
    }

    private PromptLocatorEvidence toComponentLocatorEvidence(
            SemanticComponentModel component,
            ScopedLocatorCandidate candidate,
            PageElementModel element
    ) {
        String elementName = firstNonBlank(
                elementNameFromPageModel(element, new PageLocatorModel(candidate.strategy(), candidate.value(), candidate.finalScore(), "component scoped locator", candidate.uniqueOnPage())),
                element.semanticType(),
                element.technicalType(),
                "element"
        );
        return new PromptLocatorEvidence(
                fieldHint(elementName),
                fieldHint(elementName),
                normalizeStrategy(candidate.strategy()),
                candidate.value(),
                normalizeRole(firstNonBlank(element.role(), element.semanticType(), element.technicalType())),
                element.text(),
                element.href(),
                true,
                candidate.finalScore(),
                component.name(),
                component.type().name(),
                candidate.globalMatchCount(),
                candidate.scopedMatchCount(),
                candidate.uniqueWithinComponent(),
                List.of("component-locator:" + component.componentId(), "element:" + element.elementId())
        );
    }

    private List<PromptLocatorEvidence> pageModelLocatorEvidence(AiContextPackage context, MappedPage targetPage) {
        if (context.pageModelBundle() == null || context.pageModelBundle().pages().isEmpty()) {
            return List.of();
        }
        return context.pageModelBundle().pages().stream()
                .filter(page -> pageModelMatchesTarget(page, targetPage))
                .findFirst()
                .map(page -> page.elements().stream()
                        .map(element -> selectPromptSafePageModelLocator(element, targetPage)
                                .map(locator -> toLocatorEvidence(element, locator))
                                .orElse(null))
                        .filter(locator -> locator != null)
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(),
                                locators -> preferTargetRouteLocators(locators, targetPage))))
                .orElse(List.of());
    }

    private List<PromptLocatorEvidence> preferTargetRouteLocators(
            List<PromptLocatorEvidence> locators,
            MappedPage targetPage
    ) {
        List<PromptLocatorEvidence> routeLocators = locators.stream()
                .filter(locator -> locatorMatchesTargetRoute(locator, targetPage))
                .toList();
        return routeLocators.isEmpty() ? locators.stream().limit(8).toList() : routeLocators.stream().limit(4).toList();
    }

    private boolean locatorMatchesTargetRoute(PromptLocatorEvidence locator, MappedPage targetPage) {
        String targetRoute = route(targetPage);
        return RouteCanonicalizer.routeEqualsOrSuffix(locator.href(), targetRoute)
                || RouteCanonicalizer.routeEqualsOrSuffix(locator.value(), targetRoute);
    }

    private boolean pageModelMatchesTarget(PageModel page, MappedPage targetPage) {
        if (page == null || targetPage == null) {
            return false;
        }
        if (!page.pageId().isBlank() && page.pageId().equalsIgnoreCase(targetPage.pageId())) {
            return true;
        }
        String targetRoute = route(targetPage);
        return RouteCanonicalizer.routeEqualsOrSuffix(page.route(), targetRoute)
                || RouteCanonicalizer.routeEqualsOrSuffix(page.url(), targetRoute)
                || RouteCanonicalizer.routeEqualsOrSuffix(page.route(), targetPage.url())
                || RouteCanonicalizer.routeEqualsOrSuffix(page.url(), targetPage.url());
    }

    private java.util.Optional<PageLocatorModel> selectPromptSafePageModelLocator(PageElementModel element, MappedPage targetPage) {
        if (element == null || !element.visible() || element.locatorCandidates().isEmpty()) {
            return java.util.Optional.empty();
        }
        return element.locatorCandidates().stream()
                .filter(locator -> promptSafePageModelLocator(element, locator, targetPage))
                .max(Comparator.comparingDouble(locator -> locator.score() + pageModelStrategyBonus(locator.strategy())));
    }

    private boolean promptSafePageModelLocator(PageElementModel element, PageLocatorModel locator, MappedPage targetPage) {
        if (locator == null || locator.value().isBlank()) {
            return false;
        }
        if (!locator.stableAcrossRuns()) {
            return false;
        }
        if (locator.score() < 0.75d) {
            return false;
        }
        String value = locator.value().toLowerCase(Locale.ROOT);
        String evidence = String.join(" ",
                element.elementId(),
                element.semanticType(),
                element.technicalType(),
                element.tag(),
                element.inputType(),
                element.name(),
                element.id(),
                element.placeholder(),
                element.ariaLabel(),
                element.role(),
                element.href(),
                element.text(),
                locator.reason()
        ).toLowerCase(Locale.ROOT);
        if (containsAny(evidence, "csrf", "xsrf", "token", "_token", "authenticity_token")) {
            return false;
        }
        if (containsAny(value, "/html[", "body/", "following-sibling", "preceding-sibling")) {
            return false;
        }
        if (externalHref(element.href(), targetPage) || externalLocatorValue(locator.value(), targetPage)) {
            return false;
        }
        return !containsAny(evidence, "external-origin", "hidden-or-invisible", "security-token", "semantic-locator-conflict");
    }

    private PromptLocatorEvidence toLocatorEvidence(PageElementModel element, PageLocatorModel locator) {
        String elementName = firstNonBlank(
                elementNameFromPageModel(element, locator),
                element.semanticType(),
                element.technicalType(),
                "element"
        );
        return new PromptLocatorEvidence(
                fieldHint(elementName),
                fieldHint(elementName),
                normalizeStrategy(locator.strategy()),
                locator.value(),
                normalizeRole(firstNonBlank(element.role(), element.semanticType(), element.technicalType())),
                element.text(),
                element.href(),
                true,
                locator.score(),
                List.of("page-model-locator:" + element.elementId())
        );
    }

    private String elementNameFromPageModel(PageElementModel element, PageLocatorModel locator) {
        String value = locator.value();
        if (!element.name().isBlank() && !isSecurityTokenName(element.name())) {
            return element.name();
        }
        if (!element.id().isBlank()) {
            return element.id();
        }
        if (!element.ariaLabel().isBlank()) {
            return element.ariaLabel();
        }
        if (!element.placeholder().isBlank()) {
            return element.placeholder();
        }
        if (!element.text().isBlank()) {
            return element.text();
        }
        if (!element.href().isBlank()) {
            String route = RouteCanonicalizer.canonicalize(element.href());
            if (!route.isBlank() && !"/".equals(route)) {
                String[] parts = route.split("/");
                return parts[parts.length - 1];
            }
        }
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]+", " ").trim();
    }

    private boolean isSecurityTokenName(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("_token")
                || normalized.contains("csrf")
                || normalized.contains("xsrf")
                || normalized.contains("authenticity");
    }

    private String normalizeStrategy(String strategy) {
        String normalized = strategy == null ? "" : strategy.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private double pageModelStrategyBonus(String strategy) {
        String normalized = normalizeStrategy(strategy);
        if (normalized.equals("id")) {
            return 0.05d;
        }
        if (normalized.equals("name")) {
            return 0.04d;
        }
        if (normalized.equals("css")) {
            return 0.03d;
        }
        if (normalized.equals("xpath")) {
            return -0.10d;
        }
        return 0.0d;
    }

    private boolean externalHref(String href, MappedPage targetPage) {
        String normalized = href == null ? "" : href.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("//")) {
            return true;
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            return false;
        }
        return !sameOrigin(normalized, targetPage.url());
    }

    private boolean externalLocatorValue(String value, MappedPage targetPage) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("//") && !normalized.contains("http://") && !normalized.contains("https://")) {
            return true;
        }
        String absoluteUrl = extractAbsoluteUrl(normalized);
        return !absoluteUrl.isBlank() && !sameOrigin(absoluteUrl, targetPage.url());
    }

    private String extractAbsoluteUrl(String value) {
        int http = value.indexOf("http://");
        int https = value.indexOf("https://");
        int start = http >= 0 ? http : https;
        if (start < 0) {
            return "";
        }
        int end = value.length();
        for (int index = start; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\'' || ch == '"' || Character.isWhitespace(ch) || ch == ']') {
                end = index;
                break;
            }
        }
        return value.substring(start, end);
    }

    private boolean sameOrigin(String candidateUrl, String targetUrl) {
        try {
            URI candidate = URI.create(candidateUrl);
            URI target = URI.create(targetUrl == null ? "" : targetUrl.trim());
            return candidate.getScheme() != null
                    && target.getScheme() != null
                    && candidate.getScheme().equalsIgnoreCase(target.getScheme())
                    && safe(candidate.getHost()).equalsIgnoreCase(safe(target.getHost()))
                    && port(candidate) == port(target);
        } catch (Exception ignored) {
            return false;
        }
    }

    private int port(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme.equals("https")) {
            return 443;
        }
        if (scheme.equals("http")) {
            return 80;
        }
        return -1;
    }

    private boolean containsAny(String value, String... fragments) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private PromptLocatorEvidence toLocatorEvidence(
            String elementName,
            String role,
            String visibleText,
            LocatorCandidate locator
    ) {
        String normalizedName = normalizeElementName(elementName, locator, role, visibleText);
        return new PromptLocatorEvidence(
                fieldHint(normalizedName),
                normalizedName,
                locator.strategy().wireName(),
                locator.value(),
                normalizeRole(role.isBlank() ? locator.elementRole() : role),
                visibleText.isBlank() ? locator.visibleText() : visibleText,
                locator.href(),
                locator.sameOrigin(),
                locator.stabilityScore(),
                List.of("curated-locator:" + locator.evidenceSource())
        );
    }

    private List<PromptLocatorEvidence> deduplicateLocators(List<PromptLocatorEvidence> locators) {
        Map<String, PromptLocatorEvidence> deduped = new LinkedHashMap<>();
        for (PromptLocatorEvidence locator : locators) {
            String key = locator.strategy().toLowerCase(Locale.ROOT) + "::"
                    + locator.value().toLowerCase(Locale.ROOT);
            deduped.putIfAbsent(key, locator);
        }
        return new ArrayList<>(deduped.values());
    }

    private String normalizeElementName(
            String elementName,
            LocatorCandidate locator,
            String role,
            String visibleText
    ) {
        String candidate = firstNonBlank(elementName, visibleText, locator == null ? "" : locator.accessibleName());
        String normalized = candidate == null ? "" : candidate.trim();
        if (isGenericElementName(normalized)) {
            normalized = firstNonBlank(
                    visibleText,
                    locator == null ? "" : locator.accessibleName(),
                    semanticNameFromLocator(locator),
                    normalized
            );
        }
        if (normalized.isBlank()) {
            normalized = semanticNameFromLocator(locator);
        }
        return fieldHint(normalized);
    }

    private boolean isGenericElementName(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        return normalized.isBlank()
                || normalized.equals("field")
                || normalized.equals("input")
                || normalized.equals("password")
                || normalized.equals("passwordinput")
                || normalized.equals("button")
                || normalized.equals("link")
                || normalized.equals("element");
    }

    private String semanticNameFromLocator(LocatorCandidate locator) {
        if (locator == null) {
            return "element";
        }
        String value = locator.value();
        String normalized = value == null ? "" : value.trim();
        if (normalized.contains("=")) {
            normalized = normalized.substring(normalized.lastIndexOf('=') + 1);
        }
        normalized = normalized.replaceAll("^[\"'\\[]+|[\"'\\]]+$", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (normalized.equalsIgnoreCase("submit")) {
            return "loginButton";
        }
        return normalized.isBlank() ? "element" : normalized;
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.contains("password")) {
            return "password";
        }
        if (normalized.contains("input") || normalized.contains("field") || normalized.contains("text")) {
            return "input";
        }
        if (normalized.contains("button") || normalized.contains("submit")) {
            return "button";
        }
        if (normalized.contains("link")) {
            return "link";
        }
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private String route(MappedPage page) {
        return page.urlPattern().isBlank() ? page.url() : page.urlPattern();
    }

    private String ownerPage(CanonicalTestCase testCase) {
        return testCase.sourcePageName().isBlank() ? testCase.pageName() : testCase.sourcePageName();
    }

    private List<PromptActionEvidence> deduplicateActions(List<PromptActionEvidence> actions) {
        Set<String> keys = new LinkedHashSet<>();
        List<PromptActionEvidence> deduped = new ArrayList<>();
        for (PromptActionEvidence action : actions) {
            String key = action.name() + "|" + action.type() + "|" + action.ownerPage();
            if (keys.add(key)) {
                deduped.add(action);
            }
        }
        return deduped;
    }

    private double confidence(AiContextPackage context, List<PromptLocatorEvidence> locators, List<PromptAssertionEvidence> assertions) {
        double locatorConfidence = locators.stream()
                .mapToDouble(PromptLocatorEvidence::stabilityScore)
                .average()
                .orElse(0.0d);
        double assertionConfidence = assertions.stream()
                .mapToDouble(PromptAssertionEvidence::confidence)
                .average()
                .orElse(0.0d);
        double curatedConfidence = context.mappedUiKnowledgeCurated() == null ? 0.0d : context.mappedUiKnowledgeCurated().confidence();
        return Math.max(locatorConfidence, Math.max(assertionConfidence, curatedConfidence));
    }

    private String fieldHint(String value) {
        String normalized = value == null ? "" : value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "element";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            builder.append(parts[index].substring(0, 1).toUpperCase()).append(parts[index].substring(1));
        }
        return builder.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }
}
