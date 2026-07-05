package ua.demo.agentlab.ai.ui.prompt;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.prompt.scope.PomScopeSanitizer;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds a compact ownership contract for one generated Page Object prompt.
 */
public class PageObjectCapabilityContractFormatter {

    private final PomScopeSanitizer pomScopeSanitizer = new PomScopeSanitizer();

    public String format(AiContextPackage context, String requestedPageName, AiPageObjectSpec baselineSpec) {
        return format(context, requestedPageName, List.of(), baselineSpec);
    }

    public String format(
            AiContextPackage context,
            String requestedPageName,
            List<UiTestScenario> pageScenarios,
            AiPageObjectSpec baselineSpec
    ) {
        String pageName = requestedPageName == null || requestedPageName.isBlank()
                ? "RequestedPage"
                : requestedPageName.trim();
        String route = baselineSpec == null || baselineSpec.route().isBlank()
                ? resolveRoute(context, pageName)
                : baselineSpec.route();
        String openMethod = baselineSpec == null || baselineSpec.openMethodName().isBlank()
                ? "open" + pageName.replaceAll("[^A-Za-z0-9]", "")
                : baselineSpec.openMethodName();

        String capability = resolveCapability(context, pageName);
        Contract contract = new Contract(pageName, route, openMethod, capability);
        boolean hasSemanticActions = hasSemanticActionEvidence(context);
        addPromptEvidence(contract, context);
        if (context != null && context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                addTestCaseOwnership(contract, testCase, pageName, !hasSemanticActions);
            }
        }
        addBaselineMethods(contract, baselineSpec);
        addForbiddenMethods(contract);
        contract.applyPromptReadyScope(pomScopeSanitizer.sanitize(context, pageName, pageScenarios));
        return contract.render();
    }

    public String capabilityFor(AiContextPackage context, String requestedPageName) {
        String pageName = requestedPageName == null || requestedPageName.isBlank()
                ? "RequestedPage"
                : requestedPageName.trim();
        return resolveCapability(context, pageName);
    }

    private boolean hasSemanticActionEvidence(AiContextPackage context) {
        return context != null
                && context.promptUiEvidence() != null
                && context.promptUiEvidence().requiredActions().stream()
                .anyMatch(action -> action.type().startsWith("semantic-"));
    }

    private void addPromptEvidence(Contract contract, AiContextPackage context) {
        if (context == null || context.promptUiEvidence() == null) {
            return;
        }
        context.promptUiEvidence().requirementIds().forEach(contract.coveredTestCases::add);
        context.promptUiEvidence().requiredActions().forEach(action -> {
            if (!action.name().isBlank()) {
                addPromptAction(contract, action.name(), action.type());
            }
        });
        context.promptUiEvidence().requiredAssertions().forEach(assertion -> {
            if (!assertion.type().isBlank()) {
                contract.ownedAssertions.add(assertion.type() + "(" + assertion.expectedValue() + ")");
            }
        });
        context.promptUiEvidence().requiredLocators().forEach(locator -> {
            if (!locator.fieldHint().isBlank()) {
                contract.requiredLocators.add(locator.fieldHint()
                        + "[" + locator.strategy() + "=" + locator.value() + "]");
            }
        });
    }

    private void addPromptAction(Contract contract, String actionName, String actionType) {
        if (actionType != null && actionType.startsWith("semantic-")) {
            addSemanticOwnedAction(contract, actionName, actionType);
            return;
        }
        contract.ownedActions.add(actionName);
    }

    private void addSemanticOwnedAction(Contract contract, String actionName, String actionType) {
        String normalized = normalizeActionName(actionName);
        if (normalized.isBlank()) {
            return;
        }
        if (containsAny(normalized, "authentication", "authenticate")) {
            if (!isAuthenticationContractPage(contract)) {
                contract.prerequisitePages.add("LoginPage");
                return;
            }
            addAuthenticationContract(contract);
            return;
        }
        if (containsAny(normalized, "logout", "signout")) {
            contract.ownedActions.add("logout()");
            contract.requiredLocators.add("logoutLink");
            return;
        }
        if (containsAny(normalized, "search")) {
            contract.ownedActions.add("search(String query)");
            contract.requiredLocators.add("searchInputOrButton");
            return;
        }
        if (normalized.endsWith(":type")) {
            if (!isAuthenticationContractPage(contract) && containsAny(normalized, "username", "password")) {
                return;
            }
            String element = normalized.substring(0, normalized.length() - ":type".length());
            if (containsAny(element, "username", "email", "user")) {
                contract.ownedActions.add("enterUsername(String username)");
                contract.requiredLocators.add("usernameInput");
            } else if (containsAny(element, "password", "pass")) {
                contract.ownedActions.add("enterPassword(String password)");
                contract.requiredLocators.add("passwordInput");
            } else if (!element.isBlank()) {
                contract.ownedActions.add("enter" + methodSuffix(element) + "(String value)");
                contract.requiredLocators.add(element + "Input");
            }
            return;
        }
        if (normalized.endsWith(":click")) {
            String element = normalized.substring(0, normalized.length() - ":click".length());
            if (containsAny(element, "login", "submit", "signin")) {
                if (!isAuthenticationContractPage(contract)) {
                    return;
                }
                contract.ownedActions.add("clickLoginButton()");
                contract.requiredLocators.add("loginButton");
            } else if (!element.isBlank()) {
                contract.ownedActions.add("click" + methodSuffix(element) + "()");
                contract.requiredLocators.add(element + "Control");
            }
            return;
        }
        if (normalized.endsWith(":submitform")
                || normalized.endsWith(":submit-form")
                || normalized.endsWith(":submit_form")) {
            if (isLoginPage(contract.pageName) || contract.capability.equals("AUTHENTICATION")) {
                addAuthenticationContract(contract);
            } else {
                contract.ownedActions.add("submitForm()");
            }
            return;
        }
        if (containsAny(normalized, "navigate")) {
            contract.ownedActions.add("openNavigationTarget()");
        }
    }

    private void addAuthenticationContract(Contract contract) {
        if (!isAuthenticationContractPage(contract)) {
            contract.prerequisitePages.add("LoginPage");
            return;
        }
        contract.ownedActions.add("enterUsername(String username)");
        contract.ownedActions.add("enterPassword(String password)");
        contract.ownedActions.add("clickLoginButton()");
        contract.ownedActions.add("login(String username, String password)");
        contract.requiredLocators.add("usernameInput");
        contract.requiredLocators.add("passwordInput");
        contract.requiredLocators.add("loginButton");
    }

    private boolean isAuthenticationContractPage(Contract contract) {
        return contract != null
                && (isLoginPage(contract.pageName)
                || "AUTHENTICATION".equals(contract.capability)
                || containsAny(contract.route, "login", "auth/login"));
    }

    private String methodSuffix(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (normalized.isBlank()) {
            return "Element";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? "Element" : builder.toString();
    }

    private String normalizeActionName(String value) {
        return value == null ? "" : value.trim()
                .replace('_', '-')
                .toLowerCase(Locale.ROOT)
                .replace(" ", "");
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

    private void addTestCaseOwnership(
            Contract contract,
            CanonicalTestCase testCase,
            String requestedPageName,
            boolean includeCanonicalActions
    ) {
        boolean sourcePage = PageReferenceMatcher.matchesScenarioPage(
                testCase.sourcePageName(),
                testCase.sourceRoute(),
                requestedPageName
        );
        boolean targetPage = PageReferenceMatcher.matchesScenarioPage(
                testCase.pageName(),
                testCase.route(),
                requestedPageName
        );
        if (!sourcePage && !targetPage) {
            return;
        }

        contract.coveredTestCases.add(testCase.id());
        if (sourcePage) {
            contract.ownedActionTestCases.add(testCase.id());
            if (includeCanonicalActions) {
                testCase.operationIntents().forEach(intent -> addOwnedAction(contract, intent, testCase));
                addTextDrivenSourceActions(contract, testCase);
            }
        }
        if (targetPage) {
            contract.ownedAssertionTestCases.add(testCase.id());
            testCase.assertionIntents().forEach(intent -> addOwnedAssertion(contract, intent));
            addTextDrivenTargetAssertions(contract, testCase);
            addPrerequisite(contract, testCase);
        }
    }

    private void addOwnedAction(Contract contract, UiOperationIntent intent, CanonicalTestCase testCase) {
        if (intent == null || intent.kind() == null) {
            return;
        }
        switch (intent.kind()) {
            case AUTHENTICATE -> addAuthenticationContract(contract);
            case SUBMIT_FORM -> contract.ownedActions.add("submitForm()");
            case OPEN_DETAILS -> contract.ownedActions.add("openEntityDetails(String entityKey)");
            case OPEN_TARGET_CONTAINER, OPEN_DESTINATION_CONTAINER -> contract.ownedActions.add("openTargetContainer()");
            case ADD_ENTITY_TO_CONTAINER, ADD_ITEM_TO_CONTAINER -> contract.ownedActions.add("addEntityToContainer(String entityKey)");
            case REMOVE_ENTITY_FROM_CONTAINER, REMOVE_ITEM_FROM_CONTAINER -> contract.ownedActions.add("removeEntityFromContainer(String entityKey)");
            case SEARCH -> contract.ownedActions.add("search(String query)");
            case FILTER -> contract.ownedActions.add("filter(String value)");
            case LOGOUT -> contract.ownedActions.add("logout()");
            case UPLOAD_FILE -> contract.ownedActions.add("uploadFile(String path)");
            case DOWNLOAD_FILE -> contract.ownedActions.add("downloadFile()");
            case INSPECT_COLLECTION, INSPECT_LISTING, INSPECT_ITEM_CARDS ->
                    contract.ownedAssertions.add("hasVisibleItems()");
            case OPEN_PAGE, VERIFY_PAGE_ACCESS, INSPECT_PAGE_CONTENT, INSPECT_ENTITY_SUMMARY,
                    REVIEW_ENTITY_CONTENT, REVIEW_ITEM_CONTENT, VERIFY_PUBLIC_ACCESS, SORT -> {
                // Covered by openMethodName or assertion methods unless text-driven heuristics below add more.
            }
        }
        addTextDrivenSourceActions(contract, testCase);
    }

    private void addOwnedAssertion(Contract contract, AssertionIntent intent) {
        if (intent == null || intent.kind() == null) {
            return;
        }
        AssertionIntentKind kind = intent.kind();
        switch (kind) {
            case PAGE_VISIBLE, PAGE_ACCESSIBLE, CONTENT_VISIBLE, PUBLIC_ACCESSIBLE, SUCCESS_STATE_VISIBLE ->
                    contract.ownedAssertions.add("isSuccessStateVisible()");
            case URL_CONTAINS -> contract.ownedAssertions.add("isCurrentRoute()");
            case ERROR_VISIBLE, AUTH_REQUIRED -> contract.ownedAssertions.add("isErrorVisible()");
            case COLLECTION_VISIBLE, LISTING_VISIBLE, ITEM_CARDS_VISIBLE, NON_EMPTY_RESULTS ->
                    contract.ownedAssertions.add("hasVisibleItems()");
            case ENTITY_SUMMARY_VISIBLE -> contract.ownedAssertions.add("hasEntitySummary(String entityKey)");
            case ENTITY_CONTENT_VISIBLE, ITEM_CONTENT_VISIBLE, DETAILS_VISIBLE, DETAILS_OPENED ->
                    contract.ownedAssertions.add("hasEntityContent(String entityKey)");
            case ENTITY_PRESENT_IN_CONTAINER, ITEM_PRESENT_IN_CONTAINER ->
                    contract.ownedAssertions.add("hasEntityInTargetContainer(String entityKey)");
            case CONTAINER_EMPTY -> contract.ownedAssertions.add("isTargetContainerEmpty()");
        }
    }

    private void addTextDrivenSourceActions(Contract contract, CanonicalTestCase testCase) {
        String text = normalizedText(testCase);
        if (text.contains("authentication entry point") || text.contains("auth entry point")) {
            contract.ownedActions.add("openAuthenticationEntryPoint()");
            contract.requiredLocators.add("authenticationEntryPointLink[href contains login route]");
            contract.ownedAssertions.add("hasAuthenticationEntryPointToLogin()");
        }
        if (text.contains("logout")) {
            contract.ownedActions.add("logout()");
            contract.requiredLocators.add("logoutLink");
        }
    }

    private void addTextDrivenTargetAssertions(Contract contract, CanonicalTestCase testCase) {
        String text = normalizedText(testCase);
        if (text.contains("welcome")) {
            contract.ownedAssertions.add("hasWelcomeMessage()");
            contract.requiredLocators.add("welcomeMessageOrHeading");
        }
        if (text.contains("authenticated area") || text.contains("secure area")) {
            contract.ownedAssertions.add("isAuthenticatedAreaVisible()");
        }
        if (isLoginPage(contract.pageName) && text.contains("login page")) {
            contract.ownedAssertions.add("isLoginFormVisible()");
        }
    }

    private void addPrerequisite(Contract contract, CanonicalTestCase testCase) {
        if (testCase.sourcePageName() == null || testCase.sourcePageName().isBlank()) {
            return;
        }
        if (!PageReferenceMatcher.matchesScenarioPage(testCase.sourcePageName(), testCase.sourceRoute(), contract.pageName)) {
            contract.prerequisitePages.add(testCase.sourcePageName());
        }
    }

    private void addBaselineMethods(Contract contract, AiPageObjectSpec baselineSpec) {
        if (baselineSpec == null || baselineSpec.methods() == null) {
            return;
        }
        baselineSpec.methods().forEach(method -> {
            if (method.methodName() != null && !method.methodName().isBlank()) {
                contract.baselineMethods.add(method.methodName());
            }
        });
    }

    private void addForbiddenMethods(Contract contract) {
        boolean ownsLogin = contract.ownedActions.stream().anyMatch(method -> method.startsWith("login("));
        if (!ownsLogin) {
            contract.forbiddenMethods.add("login");
            contract.forbiddenMethods.add("enterUsername");
            contract.forbiddenMethods.add("enterPassword");
            contract.forbiddenMethods.add("submitLogin");
            contract.forbiddenLocators.add("usernameInput");
            contract.forbiddenLocators.add("passwordInput");
            contract.forbiddenLocators.add("loginButton");
        }
        boolean ownsTargetContainer = contract.ownedActions.stream().anyMatch(method -> method.contains("TargetContainer"))
                || contract.ownedAssertions.stream().anyMatch(method -> method.contains("TargetContainer"));
        if (!ownsTargetContainer) {
            contract.forbiddenMethods.add("openTargetContainer");
            contract.forbiddenMethods.add("addEntityToContainer");
            contract.forbiddenMethods.add("removeEntityFromContainer");
        }
    }

    private String resolveRoute(AiContextPackage context, String pageName) {
        if (context != null
                && context.promptUiEvidence() != null
                && !context.promptUiEvidence().targetRoute().isBlank()) {
            return context.promptUiEvidence().targetRoute();
        }
        if (context != null && context.mappedUiKnowledge() != null) {
            return context.mappedUiKnowledge().pages().stream()
                    .filter(page -> PageReferenceMatcher.matches(page, pageName))
                    .map(page -> page.urlPattern() == null || page.urlPattern().isBlank() ? page.url() : page.urlPattern())
                    .filter(route -> route != null && !route.isBlank())
                    .findFirst()
                    .orElseGet(() -> resolveRouteFromFlow(context, pageName));
        }
        return resolveRouteFromFlow(context, pageName);
    }

    private String resolveRouteFromFlow(AiContextPackage context, String pageName) {
        if (context != null && context.canonicalPageFlowModel() != null) {
            return context.canonicalPageFlowModel().pages().stream()
                    .filter(page -> PageReferenceMatcher.matches(page, pageName))
                    .map(page -> page.route() == null ? "" : page.route())
                    .filter(route -> !route.isBlank())
                    .findFirst()
                    .orElse("/");
        }
        return "/";
    }

    private String resolveCapability(AiContextPackage context, String pageName) {
        if (context == null || context.mappedUiKnowledge() == null) {
            return "UNKNOWN";
        }
        return context.mappedUiKnowledge().pages().stream()
                .filter(page -> PageReferenceMatcher.matches(page, pageName))
                .findFirst()
                .map(this::capabilityName)
                .orElse("UNKNOWN");
    }

    private String capabilityName(MappedPage page) {
        if (page == null || page.canonicalPageType() == null) {
            return "UNKNOWN";
        }
        return switch (page.canonicalPageType()) {
            case LANDING -> "NAVIGATION";
            case LISTING -> "RECORD_LIST";
            case DETAILS -> "RECORD_DETAILS";
            case CART -> "CONTAINER";
            case AUTHENTICATION -> "AUTHENTICATION";
            case REGISTRATION -> "REGISTRATION";
            case RECOVERY -> "RECOVERY";
            case AUTHENTICATED_AREA -> "AUTHENTICATED_AREA";
            case SECURITY -> "SECURITY";
            case DASHBOARD -> "DASHBOARD";
            case FORM -> "FORM";
            case SEARCH -> "RECORD_LIST";
            case GENERIC -> "GENERIC";
        };
    }

    private boolean isLoginPage(String pageName) {
        return PageReferenceMatcher.normalize(pageName).contains("login");
    }

    private String normalizedText(CanonicalTestCase testCase) {
        return String.join(" ",
                        safe(testCase.title()),
                        String.join(" ", safeList(testCase.actions())),
                        String.join(" ", safeList(testCase.assertions())))
                .toLowerCase(Locale.ROOT);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class Contract {
        private final String pageName;
        private final String route;
        private final String openMethod;
        private final String capability;
        private final Set<String> coveredTestCases = new LinkedHashSet<>();
        private final Set<String> ownedActionTestCases = new LinkedHashSet<>();
        private final Set<String> ownedAssertionTestCases = new LinkedHashSet<>();
        private final Set<String> ownedActions = new LinkedHashSet<>();
        private final Set<String> ownedAssertions = new LinkedHashSet<>();
        private final Set<String> requiredLocators = new LinkedHashSet<>();
        private final Set<String> prerequisitePages = new LinkedHashSet<>();
        private final Set<String> baselineMethods = new LinkedHashSet<>();
        private final Set<String> forbiddenMethods = new LinkedHashSet<>();
        private final Set<String> forbiddenLocators = new LinkedHashSet<>();
        private final Set<String> rejectedSuggestions = new LinkedHashSet<>();

        private Contract(String pageName, String route, String openMethod, String capability) {
            this.pageName = pageName;
            this.route = route;
            this.openMethod = openMethod;
            this.capability = capability == null || capability.isBlank() ? "UNKNOWN" : capability;
        }

        private void applyPromptReadyScope(PromptReadyPomScope scope) {
            if (scope == null) {
                return;
            }
            ownedActions.clear();
            ownedActions.addAll(scope.ownedActions());
            ownedAssertions.clear();
            scope.ownedAssertions().forEach(assertion -> ownedAssertions.add(assertion.type() + "(" + assertion.expectedValue() + ")"));
            requiredLocators.clear();
            scope.allowedLocators().forEach(locator -> requiredLocators.add(locator.id()));
            rejectedSuggestions.clear();
            rejectedSuggestions.addAll(scope.rejectedSuggestions());
        }

        private String render() {
            StringBuilder builder = new StringBuilder();
            builder.append("- pageName=").append(pageName)
                    .append(" | route=").append(route)
                    .append(" | openMethodName=").append(openMethod)
                    .append(System.lineSeparator());
            builder.append("- confirmedCapability=").append(capability)
                    .append(" | pageContract=page with ").append(capability).append(" capability")
                    .append(System.lineSeparator());
            builder.append("- coveredTestCases=").append(emptyAsNone(coveredTestCases)).append(System.lineSeparator());
            builder.append("- ownedActionTestCases=").append(emptyAsNone(ownedActionTestCases)).append(System.lineSeparator());
            builder.append("- ownedAssertionTestCases=").append(emptyAsNone(ownedAssertionTestCases)).append(System.lineSeparator());
            builder.append("- ownedActions=").append(emptyAsNone(ownedActions)).append(System.lineSeparator());
            builder.append("- ownedAssertions=").append(emptyAsNone(ownedAssertions)).append(System.lineSeparator());
            builder.append("- requiredLocators=").append(emptyAsNone(requiredLocators)).append(System.lineSeparator());
            builder.append("- prerequisitePages=").append(emptyAsNone(prerequisitePages)).append(System.lineSeparator());
            builder.append("- reusableBaselineMethods=").append(emptyAsNone(baselineMethods)).append(System.lineSeparator());
            builder.append("- forbiddenMethods=").append(emptyAsNone(forbiddenMethods)).append(System.lineSeparator());
            builder.append("- forbiddenLocators=").append(emptyAsNone(forbiddenLocators)).append(System.lineSeparator());
            builder.append("- rule=Generate public methods only from ownedActions and ownedAssertions. ")
                    .append("Treat reusableBaselineMethods as compatibility hints only; do not create them unless they are also owned. ")
                    .append("Prerequisite pages are context only.");
            return builder.toString();
        }

        private String emptyAsNone(Set<String> values) {
            return values.isEmpty() ? "none" : values.toString();
        }
    }
}
