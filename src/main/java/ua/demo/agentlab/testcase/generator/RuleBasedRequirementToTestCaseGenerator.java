package ua.demo.agentlab.testcase.generator;

import ua.demo.agentlab.ai.flow.BusinessFlowContext;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.catalog.ConfirmedPageRegistry;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleBasedRequirementToTestCaseGenerator implements RequirementToTestCaseGenerator {

    private static final Pattern ROUTE_PATTERN = Pattern.compile("/[a-zA-Z0-9/_\\-]+");

    @Override
    public CanonicalTestCaseBundle generate(RequirementToTestCaseInput input) {
        if (input == null || input.normalizedRequirementBundle() == null) {
            return new CanonicalTestCaseBundle("unknown-source", "NoPages", List.of(), List.of());
        }

        FlowScopedKnowledgePackage scopedKnowledge = input.flowScopedKnowledgePackage();
        BusinessFlowContext flowContext = scopedKnowledge == null ? null : scopedKnowledge.flowContext();

        PageContext pageContext = resolvePageContext(input.projectProfile(), scopedKnowledge, flowContext);
        List<CanonicalTestCase> testCases = new ArrayList<>();

        for (NormalizedRequirement requirement : input.normalizedRequirementBundle().requirements()) {
            if (!requirement.uiRelevant()) {
                continue;
            }
            testCases.add(buildCanonicalTestCase(requirement, pageContext, flowContext, scopedKnowledge));
        }

        List<CanonicalTestCase> deduplicated = deduplicate(testCases);
        List<String> pageNames = deduplicated.stream()
                .flatMap(testCase -> testCase.targetPages().stream())
                .distinct()
                .toList();

        return new CanonicalTestCaseBundle(
                input.normalizedRequirementBundle().source(),
                pageNames.isEmpty() ? pageContext.entryPageName() : pageNames.get(0),
                pageNames,
                deduplicated
        );
    }

    private CanonicalTestCase buildCanonicalTestCase(
            NormalizedRequirement requirement,
            PageContext pageContext,
            BusinessFlowContext flowContext,
            FlowScopedKnowledgePackage scopedKnowledge
    ) {
        String normalizedText = normalize(requirement.title() + " " + requirement.statement() + " " + String.join(" ", requirement.tags()));
        List<UiOperationIntent> operationIntents = inferOperationIntents(normalizedText, requirement, pageContext);
        List<AssertionIntent> assertionIntents = inferAssertionIntents(normalizedText, requirement, operationIntents, pageContext);
        List<String> targetPages = determineTargetPages(operationIntents, assertionIntents, pageContext);
        String sourcePageName = resolveSourcePageName(operationIntents, pageContext);
        String pageName = resolveExecutionPageName(operationIntents, assertionIntents, sourcePageName, pageContext);
        String sourceRoute = routeForPage(sourcePageName, pageContext);
        String route = routeForPage(pageName, pageContext);
        UiScenarioPrerequisite prerequisite = buildPrerequisite(operationIntents, sourcePageName, sourceRoute);
        UiAssertionProfile assertionProfile = resolveAssertionProfile(operationIntents, assertionIntents);
        List<String> actions = buildActions(operationIntents, sourcePageName, pageName, route);
        List<String> assertions = buildAssertions(assertionIntents, requirement, route);
        List<LocatorHint> locatorHints = buildLocatorHints(
                scopedKnowledge,
                targetPages,
                normalizedText,
                operationIntents,
                assertionIntents
        );
        List<String> llmSteps = buildLlmSteps(requirement, prerequisite, actions, assertions);

        return new CanonicalTestCase(
                requirement.id(),
                requirement.title(),
                List.of(requirement.id()),
                llmSteps,
                operationIntents,
                assertionIntents,
                targetPages,
                prerequisite,
                "flow-" + requirement.id(),
                operationIntents.isEmpty() ? UiOperationKind.INSPECT_PAGE_CONTENT.name() : operationIntents.get(0).kind().name(),
                sourcePageName,
                pageName,
                sourceRoute,
                route,
                buildPrecondition(operationIntents, pageName),
                assertionProfile,
                actions,
                assertions,
                locatorHints,
                formatSourceReference(requirement.sourceReference())
        );
    }

    private List<UiOperationIntent> inferOperationIntents(
            String normalizedText,
            NormalizedRequirement requirement,
            PageContext pageContext
    ) {
        List<UiOperationIntent> intents = new ArrayList<>();
        String explicitRoute = extractFirstRoute(normalizedText);

        if (containsAny(normalizedText, "open", "navigate", "reach", "visit", "access", "url contains", "reachable")) {
            intents.add(new UiOperationIntent(UiOperationKind.OPEN_PAGE, explicitRoute == null ? pageContext.entryRoute() : explicitRoute, null));
        }
        if (containsAny(normalizedText, "public", "without authentication", "without login", "guest", "reachable")) {
            intents.add(new UiOperationIntent(UiOperationKind.VERIFY_PAGE_ACCESS, pageContext.entryPageName(), null));
        }
        if (containsAny(normalizedText, "list", "listing", "grid", "table", "catalog", "collection", "results")) {
            intents.add(new UiOperationIntent(UiOperationKind.INSPECT_COLLECTION, pageContext.pageName(PageCapability.RECORD_LIST), null));
        }
        if (containsAny(normalizedText, "card", "tile", "row", "summary", "information", "entity information", "record information")) {
            intents.add(new UiOperationIntent(UiOperationKind.INSPECT_ENTITY_SUMMARY, pageContext.pageName(PageCapability.RECORD_LIST), null));
        }
        if (containsAny(normalizedText, "details", "open item", "open entity", "open record")) {
            intents.add(new UiOperationIntent(UiOperationKind.OPEN_DETAILS, pageContext.pageName(PageCapability.RECORD_DETAILS), "entityKey"));
        }
        if (containsAny(normalizedText, "add") && containsAny(normalizedText, "cart", "basket", "container", "wishlist", "favorites")) {
            addContainerStateSetupIntents(intents, pageContext);
            intents.add(new UiOperationIntent(UiOperationKind.ADD_ENTITY_TO_CONTAINER, pageContext.pageName(PageCapability.CONTAINER), "entityKey"));
            intents.add(new UiOperationIntent(UiOperationKind.OPEN_TARGET_CONTAINER, pageContext.pageName(PageCapability.CONTAINER), null));
        }
        if (containsAny(normalizedText, "present") && containsAny(normalizedText, "cart", "basket", "container", "wishlist", "favorites")) {
            addContainerStateSetupIntents(intents, pageContext);
            intents.add(new UiOperationIntent(UiOperationKind.OPEN_TARGET_CONTAINER, pageContext.pageName(PageCapability.CONTAINER), null));
            intents.add(new UiOperationIntent(UiOperationKind.INSPECT_ENTITY_SUMMARY, pageContext.pageName(PageCapability.CONTAINER), "entityKey"));
        }
        if (containsAny(normalizedText, "remove") && containsAny(normalizedText, "cart", "basket", "container", "wishlist", "favorites")) {
            addContainerStateSetupIntents(intents, pageContext);
            intents.add(new UiOperationIntent(UiOperationKind.OPEN_TARGET_CONTAINER, pageContext.pageName(PageCapability.CONTAINER), null));
            intents.add(new UiOperationIntent(UiOperationKind.REMOVE_ENTITY_FROM_CONTAINER, pageContext.pageName(PageCapability.CONTAINER), "entityKey"));
        }
        if (containsAny(normalizedText, "empty") && containsAny(normalizedText, "cart", "basket", "container", "wishlist", "favorites")) {
            addContainerStateSetupIntents(intents, pageContext);
            intents.add(new UiOperationIntent(UiOperationKind.OPEN_TARGET_CONTAINER, pageContext.pageName(PageCapability.CONTAINER), null));
        }
        if (containsAny(normalizedText, "search", "find", "lookup", "query")) {
            intents.add(new UiOperationIntent(UiOperationKind.SEARCH, pageContext.pageName(PageCapability.RECORD_LIST), "query"));
        }
        if (containsAny(normalizedText, "filter", "refine")) {
            intents.add(new UiOperationIntent(UiOperationKind.FILTER, pageContext.pageName(PageCapability.RECORD_LIST), "filterValue"));
        }
        if (containsAny(normalizedText, "sort", "order")) {
            intents.add(new UiOperationIntent(UiOperationKind.SORT, pageContext.pageName(PageCapability.RECORD_LIST), "sortValue"));
        }
        if (containsAny(normalizedText, "login", "sign in", "authenticate")) {
            intents.add(new UiOperationIntent(UiOperationKind.AUTHENTICATE, pageContext.pageName(PageCapability.AUTHENTICATION), null));
        }
        if (containsAny(normalizedText, "submit", "save", "send", "create", "update", "form")) {
            intents.add(new UiOperationIntent(UiOperationKind.SUBMIT_FORM, pageContext.pageName(PageCapability.FORM), "default"));
        }
        if (containsAny(normalizedText, "upload", "attach", "import")) {
            intents.add(new UiOperationIntent(UiOperationKind.UPLOAD_FILE, pageContext.pageName(PageCapability.FORM), "filePath"));
        }
        if (containsAny(normalizedText, "download", "export")) {
            intents.add(new UiOperationIntent(UiOperationKind.DOWNLOAD_FILE, pageContext.entryPageName(), null));
        }
        if (containsAny(normalizedText, "logout", "sign out", "log out")) {
            intents.add(new UiOperationIntent(UiOperationKind.LOGOUT, pageContext.entryPageName(), null));
        }

        if (intents.isEmpty()) {
            intents.add(new UiOperationIntent(UiOperationKind.OPEN_PAGE, explicitRoute == null ? pageContext.entryRoute() : explicitRoute, null));
            intents.add(new UiOperationIntent(UiOperationKind.INSPECT_PAGE_CONTENT, pageContext.entryPageName(), null));
        }

        return deduplicateOperationIntents(intents);
    }

    private void addContainerStateSetupIntents(List<UiOperationIntent> intents, PageContext pageContext) {
        intents.add(new UiOperationIntent(UiOperationKind.OPEN_PAGE, pageContext.entryRoute(), null));
        intents.add(new UiOperationIntent(UiOperationKind.OPEN_DETAILS, pageContext.pageName(PageCapability.RECORD_DETAILS), "entityKey"));
        intents.add(new UiOperationIntent(UiOperationKind.ADD_ENTITY_TO_CONTAINER, pageContext.pageName(PageCapability.CONTAINER), "entityKey"));
    }

    private List<AssertionIntent> inferAssertionIntents(
            String normalizedText,
            NormalizedRequirement requirement,
            List<UiOperationIntent> operationIntents,
            PageContext pageContext
    ) {
        List<AssertionIntent> intents = new ArrayList<>();
        String explicitRoute = extractFirstRoute(normalizedText);
        String expectedResult = firstNonBlank(requirement.expectedResult(), requirement.statement());

        if (containsAny(normalizedText, "visible", "renders", "rendered", "present", "display")) {
            intents.add(new AssertionIntent(AssertionIntentKind.PAGE_VISIBLE, expectedResult));
        }
        if (containsAny(normalizedText, "accessible", "reachable", "without authentication", "public")) {
            intents.add(new AssertionIntent(AssertionIntentKind.PAGE_ACCESSIBLE, expectedResult));
        }
        if (containsAny(normalizedText, "list", "listing", "grid", "collection")) {
            intents.add(new AssertionIntent(AssertionIntentKind.COLLECTION_VISIBLE, expectedResult));
        }
        if (containsAny(normalizedText, "card", "tile", "row")) {
            intents.add(new AssertionIntent(AssertionIntentKind.ENTITY_SUMMARY_VISIBLE, expectedResult));
        }
        if (containsAny(normalizedText, "details")) {
            intents.add(new AssertionIntent(AssertionIntentKind.DETAILS_VISIBLE, expectedResult));
        }
        if (containsAny(normalizedText, "present") && containsAny(normalizedText, "cart", "basket", "container", "wishlist", "favorites")) {
            intents.add(new AssertionIntent(AssertionIntentKind.ENTITY_PRESENT_IN_CONTAINER, expectedResult));
        }
        if (containsAny(normalizedText, "empty") && containsAny(normalizedText, "cart", "basket", "container", "wishlist", "favorites")) {
            intents.add(new AssertionIntent(AssertionIntentKind.CONTAINER_EMPTY, expectedResult));
        }
        if (containsAny(normalizedText, "error", "invalid", "prevent", "disabled", "fails gracefully")) {
            intents.add(new AssertionIntent(AssertionIntentKind.ERROR_VISIBLE, expectedResult));
        }
        if (containsAny(normalizedText, "requires authentication")) {
            intents.add(new AssertionIntent(AssertionIntentKind.AUTH_REQUIRED, expectedResult));
        }
        String expectedRoute = resolveExpectedRoute(normalizedText, explicitRoute, pageContext);
        if (expectedRoute != null) {
            intents.add(new AssertionIntent(AssertionIntentKind.URL_CONTAINS, expectedRoute));
        }

        if (intents.isEmpty()) {
            boolean opensDetails = operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.OPEN_DETAILS);
            intents.add(new AssertionIntent(
                    opensDetails ? AssertionIntentKind.DETAILS_VISIBLE : AssertionIntentKind.CONTENT_VISIBLE,
                    expectedResult
            ));
        }

        return deduplicateAssertionIntents(intents);
    }

    private String resolveExpectedRoute(String normalizedText, String explicitRoute, PageContext pageContext) {
        if (explicitRoute != null) {
            return explicitRoute;
        }
        if (!containsAny(normalizedText, "route", "url")) {
            return null;
        }
        if (containsAny(normalizedText, "login", "sign in", "authentication")) {
            return pageContext.authRoute();
        }
        if (containsAny(normalizedText, "authenticated", "secure")) {
            return pageContext.secureAreaRoute();
        }
        if (containsAny(normalizedText, "home", "start")) {
            return pageContext.entryRoute();
        }
        return null;
    }

    private List<String> determineTargetPages(
            List<UiOperationIntent> operationIntents,
            List<AssertionIntent> assertionIntents,
            PageContext pageContext
    ) {
        Set<String> pages = new LinkedHashSet<>();
        boolean expectsAuthenticationError = assertionIntents.stream()
                .anyMatch(intent -> intent.kind() == AssertionIntentKind.ERROR_VISIBLE
                        || intent.kind() == AssertionIntentKind.AUTH_REQUIRED);
        for (UiOperationIntent intent : operationIntents) {
            switch (intent.kind()) {
                case OPEN_DETAILS -> {
                    pages.add(pageContext.pageName(PageCapability.RECORD_LIST));
                    pages.add(pageContext.pageName(PageCapability.RECORD_DETAILS));
                }
                case ADD_ENTITY_TO_CONTAINER -> {
                    pages.add(pageContext.pageName(PageCapability.RECORD_DETAILS));
                    pages.add(pageContext.pageName(PageCapability.CONTAINER));
                }
                case OPEN_TARGET_CONTAINER, REMOVE_ENTITY_FROM_CONTAINER -> pages.add(pageContext.pageName(PageCapability.CONTAINER));
                case AUTHENTICATE -> {
                    pages.add(pageContext.pageName(PageCapability.AUTHENTICATION));
                    if (!expectsAuthenticationError) {
                        pages.add(pageContext.pageName(PageCapability.AUTHENTICATED_AREA));
                    }
                }
                case SUBMIT_FORM, UPLOAD_FILE -> pages.add(pageContext.pageName(PageCapability.FORM));
                default -> pages.add(resolveIntentTargetPage(intent, pageContext));
            }
        }
        return List.copyOf(pages);
    }

    private String resolveIntentTargetPage(UiOperationIntent intent, PageContext pageContext) {
        if (intent != null && intent.target() != null && !intent.target().isBlank() && !intent.target().startsWith("/")) {
            return intent.target();
        }
        return pageContext.entryPageName();
    }

    private String resolveSourcePageName(List<UiOperationIntent> operationIntents, PageContext pageContext) {
        boolean startsFromListing = operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.OPEN_PAGE)
                && operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.OPEN_DETAILS);
        if (startsFromListing) {
            return pageContext.entryPageName();
        }
        boolean addsToContainer = operationIntents.stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.ADD_ENTITY_TO_CONTAINER);
        if (addsToContainer) {
            return pageContext.pageName(PageCapability.RECORD_DETAILS);
        }
        boolean removesFromContainer = operationIntents.stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.REMOVE_ENTITY_FROM_CONTAINER);
        if (removesFromContainer) {
            return pageContext.pageName(PageCapability.CONTAINER);
        }
        boolean opensTargetContainer = operationIntents.stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.OPEN_TARGET_CONTAINER);
        if (opensTargetContainer) {
            return pageContext.pageName(PageCapability.CONTAINER);
        }
        boolean opensDetails = operationIntents.stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.OPEN_DETAILS);
        if (opensDetails) {
            return pageContext.entryPageName();
        }
        boolean hasAuth = operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.AUTHENTICATE);
        if (hasAuth) {
            return pageContext.pageName(PageCapability.AUTHENTICATION);
        }
        boolean hasForm = operationIntents.stream().anyMatch(intent ->
                intent.kind() == UiOperationKind.SUBMIT_FORM || intent.kind() == UiOperationKind.UPLOAD_FILE);
        if (hasForm) {
            return pageContext.pageName(PageCapability.FORM);
        }
        return pageContext.entryPageName();
    }

    private String resolveExecutionPageName(
            List<UiOperationIntent> operationIntents,
            List<AssertionIntent> assertionIntents,
            String sourcePageName,
            PageContext pageContext
    ) {
        boolean cartAssertion = assertionIntents.stream().anyMatch(intent ->
                intent.kind() == AssertionIntentKind.ENTITY_PRESENT_IN_CONTAINER
                        || intent.kind() == AssertionIntentKind.CONTAINER_EMPTY);
        boolean removesFromContainer = operationIntents.stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.REMOVE_ENTITY_FROM_CONTAINER);
        if (cartAssertion || removesFromContainer) {
            return pageContext.pageName(PageCapability.CONTAINER);
        }
        boolean addsToContainer = operationIntents.stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.ADD_ENTITY_TO_CONTAINER);
        if (addsToContainer) {
            return pageContext.pageName(PageCapability.RECORD_DETAILS);
        }
        boolean opensDetails = operationIntents.stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.OPEN_DETAILS);
        if (opensDetails) {
            return pageContext.pageName(PageCapability.RECORD_DETAILS);
        }
        boolean hasSuccessfulAuth = operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.AUTHENTICATE)
                && assertionIntents.stream().noneMatch(intent -> intent.kind() == AssertionIntentKind.ERROR_VISIBLE
                || intent.kind() == AssertionIntentKind.AUTH_REQUIRED);
        if (hasSuccessfulAuth) {
            return pageContext.pageName(PageCapability.AUTHENTICATED_AREA);
        }
        return sourcePageName;
    }

    private UiScenarioPrerequisite buildPrerequisite(
            List<UiOperationIntent> operationIntents,
            String sourcePageName,
            String sourceRoute
    ) {
        List<String> setupActions = new ArrayList<>();
        boolean authenticationRequired = operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.AUTHENTICATE);
        setupActions.add("Open " + sourcePageName);
        if (operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.OPEN_DETAILS)) {
            setupActions.add("Identify a representative entity");
        }
        if (operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.ADD_ENTITY_TO_CONTAINER)) {
            setupActions.add("Use scenario data to select the target entity within this test");
        }
        if (operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.REMOVE_ENTITY_FROM_CONTAINER)) {
            setupActions.add("Within this test, add the selected entity to the target container before removal");
        }
        return new UiScenarioPrerequisite(sourcePageName, sourceRoute, authenticationRequired, setupActions);
    }

    private UiAssertionProfile resolveAssertionProfile(
            List<UiOperationIntent> operationIntents,
            List<AssertionIntent> assertionIntents
    ) {
        if (assertionIntents.stream().anyMatch(intent -> intent.kind() == AssertionIntentKind.URL_CONTAINS
                || intent.kind() == AssertionIntentKind.DETAILS_VISIBLE
                || intent.kind() == AssertionIntentKind.AUTH_REQUIRED)) {
            return UiAssertionProfile.NAVIGATION;
        }
        if (assertionIntents.stream().anyMatch(intent -> intent.kind() == AssertionIntentKind.ENTITY_PRESENT_IN_CONTAINER
                || intent.kind() == AssertionIntentKind.CONTAINER_EMPTY
                || intent.kind() == AssertionIntentKind.SUCCESS_STATE_VISIBLE
                || intent.kind() == AssertionIntentKind.ERROR_VISIBLE)) {
            return UiAssertionProfile.STATEFUL;
        }
        if (operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.INSPECT_ENTITY_SUMMARY
                || intent.kind() == UiOperationKind.INSPECT_PAGE_CONTENT
                || intent.kind() == UiOperationKind.SEARCH
                || intent.kind() == UiOperationKind.FILTER
                || intent.kind() == UiOperationKind.SORT)) {
            return UiAssertionProfile.CONTENT;
        }
        return UiAssertionProfile.BASIC;
    }

    private List<String> buildActions(
            List<UiOperationIntent> operationIntents,
            String sourcePageName,
            String pageName,
            String route
    ) {
        List<String> actions = new ArrayList<>();
        for (UiOperationIntent intent : operationIntents) {
            switch (intent.kind()) {
                case OPEN_PAGE -> actions.add("Open the target page" + suffixRoute(route));
                case VERIFY_PAGE_ACCESS -> actions.add("Verify the page is accessible");
                case INSPECT_PAGE_CONTENT -> actions.add("Inspect the rendered content area");
                case INSPECT_COLLECTION -> actions.add("Inspect the visible collection or result set");
                case INSPECT_ENTITY_SUMMARY -> actions.add("Inspect representative entity summaries");
                case OPEN_DETAILS -> actions.add("Open an entity details page from " + sourcePageName);
                case OPEN_TARGET_CONTAINER -> actions.add("Open the target container page");
                case ADD_ENTITY_TO_CONTAINER -> actions.add("Add the selected entity to the target container");
                case REMOVE_ENTITY_FROM_CONTAINER -> actions.add("Remove the selected entity from the target container");
                case AUTHENTICATE -> actions.add("Authenticate using the configured credentials");
                case SUBMIT_FORM -> actions.add("Submit the target form with valid data");
                case SEARCH -> actions.add("Search using a representative query");
                case FILTER -> actions.add("Apply a representative filter");
                case SORT -> actions.add("Apply a representative sort");
                case LOGOUT -> actions.add("Sign out from the current session");
                case UPLOAD_FILE -> actions.add("Upload a representative file");
                case DOWNLOAD_FILE -> actions.add("Trigger the download action");
                default -> actions.add("Interact with " + pageName + " using " + intent.kind().name());
            }
        }
        return deduplicateStrings(actions);
    }

    private List<String> buildAssertions(
            List<AssertionIntent> assertionIntents,
            NormalizedRequirement requirement,
            String route
    ) {
        List<String> assertions = new ArrayList<>();
        for (AssertionIntent intent : assertionIntents) {
            if (intent.kind() != AssertionIntentKind.URL_CONTAINS && intent.expectedValue() != null) {
                assertions.add(intent.expectedValue());
                continue;
            }
            switch (intent.kind()) {
                case PAGE_VISIBLE -> assertions.add("Target page is visible");
                case PAGE_ACCESSIBLE -> assertions.add("Target page is accessible without unexpected blocking");
                case CONTENT_VISIBLE -> assertions.add("Relevant page content is visible");
                case COLLECTION_VISIBLE -> assertions.add("The collection or result set is visible");
                case ENTITY_SUMMARY_VISIBLE -> assertions.add("At least one entity summary is visible");
                case ENTITY_CONTENT_VISIBLE, ITEM_CONTENT_VISIBLE -> assertions.add("Entity content is visible");
                case DETAILS_VISIBLE, DETAILS_OPENED -> assertions.add("The details view is visible");
                case ENTITY_PRESENT_IN_CONTAINER, ITEM_PRESENT_IN_CONTAINER -> assertions.add("The target entity is present in the container");
                case CONTAINER_EMPTY -> assertions.add("The target container is empty after the action");
                case SUCCESS_STATE_VISIBLE -> assertions.add("A success state is visible");
                case ERROR_VISIBLE -> assertions.add("An error or protective state is visible");
                case AUTH_REQUIRED -> assertions.add("Authentication is required to proceed");
                case URL_CONTAINS -> assertions.add("Current URL contains " + (intent.expectedValue() == null ? route : intent.expectedValue()));
                case NON_EMPTY_RESULTS -> assertions.add("The result set is not empty");
                default -> assertions.add(requirement.statement());
            }
        }
        if (assertions.isEmpty()) {
            assertions.add(requirement.statement());
        }
        return deduplicateStrings(assertions);
    }

    private List<String> buildLlmSteps(
            NormalizedRequirement requirement,
            UiScenarioPrerequisite prerequisite,
            List<String> actions,
            List<String> assertions
    ) {
        List<String> steps = new ArrayList<>();
        steps.add("Requirement: " + requirement.statement());
        prerequisite.setupActions().forEach(action -> steps.add("Setup: " + action));
        actions.forEach(action -> steps.add("Action: " + action));
        assertions.forEach(assertion -> steps.add("Assert: " + assertion));
        return List.copyOf(steps);
    }

    private String buildPrecondition(List<UiOperationIntent> operationIntents, String pageName) {
        if (operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.REMOVE_ENTITY_FROM_CONTAINER)) {
            return "This test creates its own container state before removal";
        }
        if (operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.ADD_ENTITY_TO_CONTAINER)) {
            return "Scenario data provides the selected entity";
        }
        if (operationIntents.stream().anyMatch(intent -> intent.kind() == UiOperationKind.OPEN_DETAILS)) {
            return pageName + " shows at least one actionable entity";
        }
        return "Application is available";
    }

    private List<LocatorHint> buildLocatorHints(
            FlowScopedKnowledgePackage scopedKnowledge,
            List<String> targetPages,
            String normalizedText,
            List<UiOperationIntent> operationIntents,
            List<AssertionIntent> assertionIntents
    ) {
        if (scopedKnowledge == null || scopedKnowledge.mappedUiKnowledge() == null) {
            return List.of();
        }

        List<String> hintTerms = new ArrayList<>();
        hintTerms.addAll(extractTerms(normalizedText));
        operationIntents.forEach(intent -> hintTerms.add(intent.kind().name().toLowerCase(Locale.ROOT)));
        assertionIntents.forEach(intent -> hintTerms.add(intent.kind().name().toLowerCase(Locale.ROOT)));

        List<LocatorHint> hints = new ArrayList<>();
        for (MappedPage page : scopedKnowledge.mappedUiKnowledge().pages()) {
            if (targetPages.stream().noneMatch(reference -> PageReferenceMatcher.matches(page, reference))) {
                continue;
            }
            for (MappedElement element : page.elements()) {
                if (!matchesHintTerms(element, hintTerms) || element.locatorCandidates().isEmpty()) {
                    continue;
                }
                LocatorCandidate candidate = element.locatorCandidates().stream()
                        .max(Comparator.comparingDouble(LocatorCandidate::stabilityScore))
                        .orElse(null);
                if (candidate != null) {
                    hints.add(new LocatorHint(element.semanticName(), candidate.strategy().wireName(), candidate.value()));
                }
            }
        }

        return hints.stream().distinct().limit(6).toList();
    }

    private List<CanonicalTestCase> deduplicate(List<CanonicalTestCase> testCases) {
        List<CanonicalTestCase> result = new ArrayList<>();
        Set<String> signatures = new LinkedHashSet<>();
        for (CanonicalTestCase testCase : testCases) {
            String signature = normalize(testCase.title() + "|" + testCase.targetPages() + "|" + testCase.operationIntents());
            if (signatures.add(signature)) {
                result.add(testCase);
            }
        }
        return List.copyOf(result);
    }

    private List<UiOperationIntent> deduplicateOperationIntents(List<UiOperationIntent> intents) {
        List<UiOperationIntent> result = new ArrayList<>();
        Set<String> signatures = new LinkedHashSet<>();
        for (UiOperationIntent intent : intents) {
            String signature = intent.kind() + "|" + safe(intent.target()) + "|" + safe(intent.dataKey());
            if (signatures.add(signature)) {
                result.add(intent);
            }
        }
        return List.copyOf(result);
    }

    private List<AssertionIntent> deduplicateAssertionIntents(List<AssertionIntent> intents) {
        List<AssertionIntent> result = new ArrayList<>();
        Set<String> signatures = new LinkedHashSet<>();
        for (AssertionIntent intent : intents) {
            String signature = intent.kind() + "|" + safe(intent.expectedValue());
            if (signatures.add(signature)) {
                result.add(intent);
            }
        }
        return List.copyOf(result);
    }

    private List<String> deduplicateStrings(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private boolean matchesHintTerms(MappedElement element, List<String> terms) {
        String haystack = normalize(element.semanticName() + " " + element.text() + " " + String.join(" ", element.supportedActions()));
        return terms.stream().anyMatch(term -> term.length() >= 4 && haystack.contains(term));
    }

    private PageContext resolvePageContext(
            ProjectProfile profile,
            FlowScopedKnowledgePackage scopedKnowledge,
            BusinessFlowContext flowContext
    ) {
        List<MappedPage> pages = scopedKnowledge == null || scopedKnowledge.mappedUiKnowledge() == null
                ? List.of()
                : scopedKnowledge.mappedUiKnowledge().pages();
        ConfirmedPageRegistry confirmedPages = new ConfirmedPageSourceResolver().resolve(profile);
        PageSlot navigation = resolveSlot(PageCapability.NAVIGATION, profile, pages, flowContext, confirmedPages);
        if (!navigation.confirmed() && !pages.isEmpty()) {
            MappedPage firstPage = pages.get(0);
            navigation = new PageSlot(PageCapability.NAVIGATION, pageName(firstPage, "HomePage"), pageRoute(firstPage, firstRouteHint(profile, flowContext, "/")), true);
        }
        PageSlot recordList = resolveSlot(PageCapability.RECORD_LIST, profile, pages, flowContext, confirmedPages).orFallback(navigation);
        PageSlot recordDetails = resolveSlot(PageCapability.RECORD_DETAILS, profile, pages, flowContext, confirmedPages).orFallback(recordList);
        PageSlot container = resolveSlot(PageCapability.CONTAINER, profile, pages, flowContext, confirmedPages).orFallback(recordList);
        PageSlot authentication = resolveSlot(PageCapability.AUTHENTICATION, profile, pages, flowContext, confirmedPages).orFallback(navigation);
        PageSlot authenticatedArea = resolveSlot(PageCapability.AUTHENTICATED_AREA, profile, pages, flowContext, confirmedPages);
        if (!authenticatedArea.confirmed()) {
            authenticatedArea = resolveSlot(PageCapability.DASHBOARD, profile, pages, flowContext, confirmedPages).orFallback(authentication);
        }
        PageSlot form = resolveSlot(PageCapability.FORM, profile, pages, flowContext, confirmedPages).orFallback(authentication.confirmed() ? authentication : navigation);

        return new PageContext(
                navigation,
                recordList,
                recordDetails,
                container,
                authentication,
                authenticatedArea,
                form
        );
    }

    private PageSlot resolveSlot(
            PageCapability capability,
            ProjectProfile profile,
            List<MappedPage> pages,
            BusinessFlowContext flowContext,
            ConfirmedPageRegistry confirmedPages
    ) {
        String route = configuredRoute(profile, capability);
        if (route != null && !route.isBlank() && confirmedPages != null) {
            var confirmed = confirmedPages.findByRoute(route);
            if (confirmed.isPresent()) {
                return new PageSlot(capability, confirmed.get().pageName(), confirmed.get().route(), true);
            }
        }
        MappedPage mappedPage = bestPage(
                pages,
                List.of(route),
                capabilityKeywords(capability),
                capability.canonicalPageType().defaultClassName(),
                capability.canonicalPageType()
        );
        if (mappedPage == null && route != null && !route.isBlank()) {
            return new PageSlot(capability, capability.defaultPageName(), route, true);
        }
        if (mappedPage != null) {
            return new PageSlot(capability, pageName(mappedPage, capability.defaultPageName()), pageRoute(mappedPage, route), true);
        }
        if (capability == PageCapability.NAVIGATION) {
            return new PageSlot(capability, "HomePage", firstRouteHint(profile, flowContext, "/"), route != null && !route.isBlank());
        }
        return new PageSlot(capability, capability.defaultPageName(), route, false);
    }

    private String configuredRoute(ProjectProfile profile, PageCapability capability) {
        if (profile == null || capability == null) {
            return "";
        }
        return switch (capability) {
            case NAVIGATION -> profile.homeRoute();
            case AUTHENTICATION -> profile.loginRoute();
            case REGISTRATION -> profile.registrationRoute();
            case RECOVERY -> profile.recoveryRoute();
            case AUTHENTICATED_AREA, DASHBOARD -> firstNonBlank(profile.authenticatedRoute(), profile.securityRoute());
            case SECURITY -> profile.securityRoute();
            case FORM -> profile.formRoute();
            case RECORD_LIST -> profile.catalogRoute();
            case RECORD_DETAILS -> firstNonBlank(profile.productsRoute(), profile.detailsRoute());
            case CONTAINER -> profile.cartRoute();
            case GENERIC -> "";
        };
    }

    private List<String> capabilityKeywords(PageCapability capability) {
        return switch (capability) {
            case NAVIGATION -> List.of("home", "landing", "navigation", "main", "start");
            case AUTHENTICATION -> List.of("login", "auth", "account", "signin", "credential");
            case AUTHENTICATED_AREA -> List.of("authenticated", "dashboard", "overview", "secure", "logout", "welcome");
            case DASHBOARD -> List.of("dashboard", "overview", "welcome");
            case FORM -> List.of("form", "submit", "field", "input", "create", "update", "edit");
            case RECORD_LIST -> List.of("list", "grid", "table", "results", "collection", "record-list", "employee-list");
            case RECORD_DETAILS -> List.of("details", "detail", "profile", "record", "summary", "information");
            case CONTAINER -> List.of("container", "basket", "selection", "queue", "wishlist", "favorites");
            case REGISTRATION -> List.of("register", "registration", "signup", "create-account");
            case RECOVERY -> List.of("recover", "recovery", "forgot", "reset");
            case SECURITY -> List.of("security", "challenge", "otp", "mfa", "verification");
            case GENERIC -> List.of("generic", "page");
        };
    }

    private String firstRouteHint(ProjectProfile profile, BusinessFlowContext flowContext, String fallback) {
        if (flowContext != null && flowContext.targetRoutes() != null) {
            for (String route : flowContext.targetRoutes()) {
                if (route != null && !route.isBlank()) {
                    return route;
                }
            }
        }
        if (profile != null
                && profile.catalogRoute() != null
                && !profile.catalogRoute().isBlank()) {
            return profile.catalogRoute();
        }
        if (profile != null
                && profile.homeRoute() != null
                && !profile.homeRoute().isBlank()) {
            return profile.homeRoute();
        }
        return fallback;
    }

    private MappedPage bestPage(
            List<MappedPage> pages,
            List<String> routeHints,
            List<String> keywords,
            String preferredClassName,
            CanonicalPageType preferredPageType
    ) {
        MappedPage bestPage = null;
        double bestScore = 0.0d;
        for (MappedPage page : pages) {
            double score = scorePage(page, routeHints, keywords, preferredClassName, preferredPageType);
            if (score > bestScore) {
                bestScore = score;
                bestPage = page;
            }
        }
        return bestPage;
    }

    private double scorePage(
            MappedPage page,
            List<String> routeHints,
            List<String> keywords,
            String preferredClassName,
            CanonicalPageType preferredPageType
    ) {
        String pageName = normalize(page.pageName());
        String pageType = normalize(page.pageType());
        String identityClassName = page.pageIdentity() == null ? "" : normalize(page.pageIdentity().className());
        String identityType = page.pageIdentity() == null || page.pageIdentity().canonicalPageType() == null
                ? ""
                : normalize(page.pageIdentity().canonicalPageType().defaultClassName());
        String preferred = normalize(preferredClassName);

        double score = 0.0d;
        if (!preferred.isBlank() && (pageName.equals(preferred) || identityClassName.equals(preferred) || identityType.equals(preferred))) {
            score += 25.0d;
        }
        if (preferredPageType != null && page.canonicalPageType() == preferredPageType) {
            score += 30.0d;
        }
        if (preferredPageType != null && page.pageIdentity() != null
                && page.pageIdentity().canonicalPageType() == preferredPageType) {
            score += 30.0d;
        }
        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            if (!normalizedKeyword.isBlank() && (pageName.contains(normalizedKeyword) || pageType.contains(normalizedKeyword))) {
                score += 8.0d;
            }
        }

        String evidence = pageEvidence(page);
        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            if (!normalizedKeyword.isBlank() && evidence.contains(normalizedKeyword)) {
                score += 3.0d;
            }
        }

        for (String routeHint : routeHints) {
            if (routeHint != null && !routeHint.isBlank()
                    && (routeEquals(page.urlPattern(), routeHint) || routeEquals(page.url(), routeHint))) {
                score += 2.0d;
            }
        }
        return score;
    }

    private String pageEvidence(MappedPage page) {
        StringBuilder builder = new StringBuilder();
        builder.append(' ').append(page.title());
        page.elements().forEach(element -> builder.append(' ')
                .append(element.semanticName())
                .append(' ')
                .append(element.elementType())
                .append(' ')
                .append(element.role())
                .append(' ')
                .append(element.text()));
        page.actions().forEach(action -> builder.append(' ')
                .append(action.actionName())
                .append(' ')
                .append(action.actionType())
                .append(' ')
                .append(action.description()));
        page.assertionHints().forEach(hint -> builder.append(' ')
                .append(hint.hintType())
                .append(' ')
                .append(hint.target())
                .append(' ')
                .append(hint.description()));
        return normalize(builder.toString());
    }

    private boolean routeEquals(String left, String right) {
        return normalizeRoute(left).equals(normalizeRoute(right));
    }

    private String normalizeRoute(String value) {
        return RouteCanonicalizer.canonicalize(value);
    }

    private String pageName(MappedPage page, String fallback) {
        return page == null || page.pageName() == null || page.pageName().isBlank() ? fallback : page.pageName();
    }

    private String pageRoute(MappedPage page, String fallback) {
        if (page == null) {
            return fallback;
        }
        if (page.urlPattern() != null && !page.urlPattern().isBlank()) {
            return page.urlPattern();
        }
        if (page.url() != null && !page.url().isBlank()) {
            return page.url();
        }
        return fallback;
    }

    private String routeForPage(String pageName, PageContext pageContext) {
        return pageContext.routeForPage(pageName);
    }

    private String formatSourceReference(SourceReference reference) {
        if (reference == null) {
            return "";
        }
        if (reference.startLine() > 0 && reference.endLine() > 0) {
            if (reference.startLine() == reference.endLine()) {
                return "%s [L%d]".formatted(reference.source(), reference.startLine());
            }
            return "%s [L%d-L%d]".formatted(reference.source(), reference.startLine(), reference.endLine());
        }
        return reference.source();
    }

    private String extractFirstRoute(String text) {
        Matcher matcher = ROUTE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private List<String> extractTerms(String text) {
        List<String> terms = new ArrayList<>();
        for (String token : normalize(text).replaceAll("[^a-z0-9/_\\- ]", " ").split("\\s+")) {
            if (!token.isBlank() && (token.length() >= 4 || token.startsWith("/"))) {
                terms.add(token);
            }
        }
        return terms;
    }

    private boolean containsAny(String haystack, String... fragments) {
        for (String fragment : fragments) {
            if (haystack.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String suffixRoute(String route) {
        return route == null || route.isBlank() ? "" : " (" + route + ")";
    }

    private record PageSlot(
            PageCapability capability,
            String pageName,
            String route,
            boolean confirmed
    ) {
        private PageSlot {
            pageName = pageName == null ? "" : pageName.trim();
            route = route == null ? "" : route.trim();
        }

        private PageSlot orFallback(PageSlot fallback) {
            if (confirmed || fallback == null) {
                return this;
            }
            return new PageSlot(capability, fallback.pageName(), fallback.route(), false);
        }
    }

    private record PageContext(
            PageSlot navigation,
            PageSlot recordList,
            PageSlot recordDetails,
            PageSlot container,
            PageSlot authentication,
            PageSlot authenticatedArea,
            PageSlot form
    ) {
        private String entryPageName() {
            return pageName(PageCapability.NAVIGATION);
        }

        private String entryRoute() {
            return route(PageCapability.NAVIGATION);
        }

        private String authRoute() {
            return route(PageCapability.AUTHENTICATION);
        }

        private String secureAreaRoute() {
            return route(PageCapability.AUTHENTICATED_AREA);
        }

        private String pageName(PageCapability capability) {
            return slot(capability).pageName();
        }

        private String route(PageCapability capability) {
            return slot(capability).route();
        }

        private String routeForPage(String pageName) {
            for (PageSlot slot : List.of(navigation, recordList, recordDetails, container, authentication, authenticatedArea, form)) {
                if (normalizeStatic(slot.pageName()).equals(normalizeStatic(pageName))) {
                    return slot.route();
                }
            }
            return navigation.route();
        }

        private PageSlot slot(PageCapability capability) {
            return switch (capability) {
                case NAVIGATION -> navigation;
                case RECORD_LIST -> recordList;
                case RECORD_DETAILS -> recordDetails;
                case CONTAINER -> container;
                case AUTHENTICATION -> authentication;
                case AUTHENTICATED_AREA, DASHBOARD -> authenticatedArea;
                case FORM, REGISTRATION, RECOVERY, SECURITY, GENERIC -> form;
            };
        }

        private static String normalizeStatic(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }
}
