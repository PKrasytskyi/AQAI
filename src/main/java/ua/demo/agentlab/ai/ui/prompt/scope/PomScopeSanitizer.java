package ua.demo.agentlab.ai.ui.prompt.scope;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PomScopeSanitizer {

    private final CanonicalLocatorIdResolver locatorIdResolver;

    public PomScopeSanitizer() {
        this(new CanonicalLocatorIdResolver());
    }

    PomScopeSanitizer(CanonicalLocatorIdResolver locatorIdResolver) {
        this.locatorIdResolver = locatorIdResolver == null ? new CanonicalLocatorIdResolver() : locatorIdResolver;
    }

    public PromptReadyPomScope sanitize(
            AiContextPackage context,
            String requestedPageName,
            List<UiTestScenario> pageScenarios
    ) {
        if (context == null || context.promptUiEvidence() == null) {
            return new PromptReadyPomScope(
                    safe(requestedPageName),
                    "",
                    false,
                    List.of(),
                    scopedIds(pageScenarios).stream().toList(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("promptUiEvidence missing"),
                    0.0d
            );
        }
        var evidence = context.promptUiEvidence();
        String targetPage = firstNonBlank(requestedPageName, evidence.targetPage());
        Set<String> scopedIds = scopedIds(pageScenarios);
        List<String> requirementIds = scopedIds.isEmpty()
                ? evidence.requirementIds()
                : evidence.requirementIds().stream().filter(scopedIds::contains).toList();
        List<String> rejected = new ArrayList<>();
        List<PromptReadyLocator> locators = canonicalLocators(evidence.requiredLocators(), targetPage, evidence.targetRoute());
        List<String> actions = ownedActions(context, targetPage, scopedIds, locators, rejected);
        List<PromptReadyAssertion> assertions = ownedAssertions(context, evidence, targetPage, scopedIds, locators, rejected);
        List<String> coverageGaps = scopeCoverageGaps(context, targetPage, evidence.targetRoute(), scopedIds, locators);
        assertions = removeAssertionsBlockedByCoverageGaps(assertions, coverageGaps);
        locators = selectContractLocators(locators, actions, assertions, targetPage, evidence.targetRoute());

        return new PromptReadyPomScope(
                targetPage,
                evidence.targetRoute(),
                evidence.requiresAuthentication(),
                evidence.prerequisitePages(),
                requirementIds,
                actions,
                assertions,
                locators,
                coverageGaps,
                rejected.stream().distinct().toList(),
                evidence.confidence()
        );
    }

    private List<String> ownedActions(
            AiContextPackage context,
            String targetPage,
            Set<String> scopedIds,
            List<PromptReadyLocator> locators,
            List<String> rejected
    ) {
        Set<String> actions = new LinkedHashSet<>();
        String targetRoute = context.promptUiEvidence() == null ? "" : context.promptUiEvidence().targetRoute();
        boolean authenticationPage = isAuthenticationPage(targetPage, targetRoute);
        boolean hasUsername = hasLocator(locators, "usernameInput");
        boolean hasPassword = hasLocator(locators, "passwordInput");
        boolean hasLoginButton = hasLocator(locators, "loginButton");
        boolean noLocatorEvidence = locators.isEmpty();

        if (context.promptUiEvidence() != null) {
            for (PromptActionEvidence action : context.promptUiEvidence().requiredActions()) {
                if (!belongsToRequestedPage(action.ownerPage(), targetPage) || !belongsToScoped(action.sourceTrace(), scopedIds)) {
                    continue;
                }
                String normalized = normalize(action.name() + " " + action.type() + " " + action.sourceTrace());
                if (isNoisyAction(normalized)) {
                    rejected.add(action.name() + ": external/non-business or generic link action");
                    continue;
                }
                if (authenticationPage && containsAny(normalized, "logout", "signout", "openusermenu", "user menu")) {
                    rejected.add(action.name() + ": belongs to authenticated-area page");
                    continue;
                }
                if (authenticationPage && containsAny(normalized, "authentication", "authenticate", "username", "password", "login")) {
                    addLoginActions(
                            actions,
                            hasUsername || noLocatorEvidence,
                            hasPassword || noLocatorEvidence,
                            hasLoginButton || noLocatorEvidence
                    );
                    continue;
                }
                String method = semanticActionToMethod(action.name() + " " + action.type() + " " + action.sourceTrace());
                if (!method.isBlank()) {
                    actions.add(method);
                }
            }
        }
        if (authenticationPage && hasUsername && hasPassword && hasLoginButton) {
            addLoginActions(actions, true, true, true);
        }
        if (!authenticationPage
                && isAuthenticatedAreaPage(targetPage, targetRoute)
                && hasLogoutMenuEvidence(context, scopedIds, targetPage, actions)) {
            if (hasLocator(locators, "userMenuTrigger") && hasLocator(locators, "logoutLink")) {
                return orderedLogoutMenuActions(actions);
            }
            actions.removeIf(action -> containsAny(normalize(action), "logout", "openusermenu", "open user menu"));
        }
        return actions.stream().limit(12).toList();
    }

    private List<String> orderedLogoutMenuActions(Set<String> actions) {
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add("openUserMenu()");
        ordered.add("logout()");
        for (String action : actions) {
            String normalized = normalize(action);
            if (!containsAny(normalized, "logout", "openusermenu", "open user menu")) {
                ordered.add(action);
            }
        }
        return ordered.stream().limit(12).toList();
    }

    private List<String> scopeCoverageGaps(
            AiContextPackage context,
            String targetPage,
            String targetRoute,
            Set<String> scopedIds,
            List<PromptReadyLocator> locators
    ) {
        if (isAuthenticationPage(targetPage, targetRoute)
                || !isAuthenticatedAreaPage(targetPage, targetRoute)
                || !hasLogoutMenuEvidence(context, scopedIds, targetPage, Set.of())) {
            return List.of();
        }
        boolean hasUserMenuTrigger = hasLocator(locators, "userMenuTrigger");
        boolean hasLogoutLink = hasLocator(locators, "logoutLink");
        if (hasUserMenuTrigger && hasLogoutLink) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        if (!hasUserMenuTrigger) {
            missing.add("userMenuTrigger");
        }
        if (!hasLogoutLink) {
            missing.add("logoutLink");
        }
        return List.of("Logout user-menu flow requires confirmed userMenuTrigger and logoutLink; missing confirmed "
                + String.join(", ", missing) + ".");
    }

    private List<PromptReadyAssertion> removeAssertionsBlockedByCoverageGaps(
            List<PromptReadyAssertion> assertions,
            List<String> coverageGaps
    ) {
        boolean logoutFlowGap = coverageGaps.stream()
                .map(this::normalize)
                .anyMatch(gap -> gap.contains("logoutlink"));
        if (!logoutFlowGap) {
            return assertions;
        }
        return assertions.stream()
                .filter(assertion -> !"logoutLink".equalsIgnoreCase(assertion.expectedValue()))
                .toList();
    }

    private void addLoginActions(Set<String> actions, boolean hasUsername, boolean hasPassword, boolean hasLoginButton) {
        if (hasUsername) {
            actions.add("enterUsername(String username)");
        }
        if (hasPassword) {
            actions.add("enterPassword(String password)");
        }
        if (hasLoginButton) {
            actions.add("clickLoginButton()");
        }
        if (hasUsername && hasPassword && hasLoginButton) {
            actions.add("login(String username, String password)");
        }
    }

    private String semanticActionToMethod(String actionName) {
        String normalized = normalize(actionName);
        if (normalized.endsWith(":click")) {
            String element = normalized.substring(0, normalized.length() - ":click".length());
            if (containsAny(element, "submit", "primary")) {
                return "clickPrimaryAction()";
            }
        }
        if (normalized.endsWith(":type")) {
            String element = normalized.substring(0, normalized.length() - ":type".length());
            if (!element.isBlank()) {
                return "enter" + methodSuffix(element) + "(String value)";
            }
        }
        if (containsAny(normalized, "search")) {
            return "search(String query)";
        }
        if (containsAny(normalized,
                "openusermenu",
                "open user menu",
                "open_menu",
                "user-menu",
                "user_menu",
                "userdropdown",
                "user-menu-trigger",
                "openmenu")) {
            return "openUserMenu()";
        }
        if (containsAny(normalized, "logout")) {
            return "logout()";
        }
        return "";
    }

    private List<PromptReadyAssertion> ownedAssertions(
            AiContextPackage context,
            ua.demo.agentlab.ai.context.PromptUiEvidence evidence,
            String targetPage,
            Set<String> scopedIds,
            List<PromptReadyLocator> locators,
            List<String> rejected
    ) {
        Map<String, PromptReadyAssertion> assertions = new LinkedHashMap<>();
        List<PromptAssertionEvidence> sourceAssertions = new ArrayList<>();
        if (context.assertionContracts() != null && !context.assertionContracts().isEmpty()) {
            for (AssertionContract contract : context.assertionContracts()) {
                if ((scopedIds.isEmpty() || scopedIds.contains(contract.testCaseId()))
                        && belongsToRequestedPage(contract.ownerPage(), targetPage)) {
                    sourceAssertions.add(new PromptAssertionEvidence(
                            contract.type().name(),
                            contract.expectedValue(),
                            contract.ownerPage(),
                            contract.sourceLine(),
                            contract.confidence()
                    ));
                } else if (isAuthenticationPage(targetPage, evidence.targetRoute())
                        && isPostLoginAssertion(contract == null ? "" : contract.expectedValue())) {
                    rejected.add("postLoginSuccessVisible: belongs to AuthenticatedAreaPage");
                }
            }
        } else {
            sourceAssertions.addAll(evidence.requiredAssertions());
        }
        for (PromptAssertionEvidence assertion : sourceAssertions) {
            if (!belongsToRequestedPage(assertion.ownerPage(), targetPage)
                    || !belongsToScoped(assertion.sourceTrace(), scopedIds)) {
                continue;
            }
            PromptReadyAssertion normalized = normalizeAssertion(assertion, targetPage, evidence.targetRoute(), locators, rejected);
            if (normalized != null) {
                assertions.putIfAbsent(normalized.type() + "|" + normalized.expectedValue(), normalized);
            }
        }
        if (isAuthenticationPage(targetPage, evidence.targetRoute())) {
            addLoginAssertions(assertions, evidence, locators);
        }
        if (!isAuthenticationPage(targetPage, evidence.targetRoute())
                && isAuthenticatedAreaPage(targetPage, evidence.targetRoute())) {
            addAuthenticatedAreaAssertions(assertions, evidence, locators);
        }
        return assertions.values().stream().limit(12).toList();
    }

    private PromptReadyAssertion normalizeAssertion(
            PromptAssertionEvidence assertion,
            String targetPage,
            String targetRoute,
            List<PromptReadyLocator> locators,
            List<String> rejected
    ) {
        String expected = assertion.expectedValue();
        String normalized = normalize(assertion.type() + " " + expected);
        if (isPostLoginAssertion(expected) && isAuthenticationPage(targetPage, targetRoute)) {
            rejected.add("postLoginSuccessVisible: belongs to AuthenticatedAreaPage");
            return null;
        }
        if (containsAny(normalized, "home page is accessible") && isAuthenticationPage(targetPage, targetRoute)) {
            rejected.add("homePageAccessible: belongs to HomePage or duplicates LoginPage route");
            return null;
        }
        if (assertion.type().equals("URL_CONTAINS")
                || assertion.type().equals("ROUTE_EQUALS")
                || containsAny(normalized, "route contains", "route matches", "current url contains")) {
            String route = routeFromAssertion(expected);
            if (!route.isBlank()
                    && !targetRoute.isBlank()
                    && !RouteCanonicalizer.routeEqualsOrSuffix(route, targetRoute)) {
                rejected.add(assertion.type() + "(" + route + "): belongs to target-after-navigation page");
                return null;
            }
            return new PromptReadyAssertion("URL_CONTAINS", route, targetPage, assertion.sourceTrace(), 1.0d);
        }
        if (containsAny(normalized, "username field")) {
            return elementAssertion("usernameInput", targetPage, assertion, locators);
        }
        if (containsAny(normalized, "password field")) {
            return elementAssertion("passwordInput", targetPage, assertion, locators);
        }
        if (containsAny(normalized, "login button")) {
            return elementAssertion("loginButton", targetPage, assertion, locators);
        }
        if (containsAny(normalized, "dashboard heading", "dashboard is visible", "successful login state")
                || assertion.type().equals("AUTHENTICATED_AREA_VISIBLE")) {
            PromptReadyAssertion heading = elementAssertion("dashboardHeading", targetPage, assertion, locators);
            if (heading != null) {
                return heading;
            }
            if (!targetRoute.isBlank()) {
                return new PromptReadyAssertion("URL_CONTAINS", targetRoute, targetPage, assertion.sourceTrace(), 1.0d);
            }
        }
        if (containsAny(normalized, "logout action")) {
            return elementAssertion("logoutLink", targetPage, assertion, locators);
        }
        if (looksLikeRequirementSentence(expected)
                || assertion.type().equals("ELEMENT_VISIBLE") && !isKnownLocatorExpectedValue(expected, locators)) {
            rejected.add(assertion.type() + "(" + expected + "): requirement sentence is not UI text");
            return null;
        }
        return new PromptReadyAssertion(assertion.type(), expected, targetPage, assertion.sourceTrace(), assertion.confidence());
    }

    private void addLoginAssertions(
            Map<String, PromptReadyAssertion> assertions,
            ua.demo.agentlab.ai.context.PromptUiEvidence evidence,
            List<PromptReadyLocator> locators
    ) {
        String route = evidence.targetRoute();
        if (!route.isBlank()) {
            add(assertions, new PromptReadyAssertion("URL_CONTAINS", route, evidence.targetPage(), "pom-scope-sanitizer:login-route", 1.0d));
        }
        if (hasLocator(locators, "usernameInput")) {
            add(assertions, new PromptReadyAssertion("ELEMENT_VISIBLE", "usernameInput", evidence.targetPage(), "pom-scope-sanitizer:username", 0.95d));
        }
        if (hasLocator(locators, "passwordInput")) {
            add(assertions, new PromptReadyAssertion("ELEMENT_VISIBLE", "passwordInput", evidence.targetPage(), "pom-scope-sanitizer:password", 0.95d));
        }
        if (hasLocator(locators, "loginButton")) {
            add(assertions, new PromptReadyAssertion("ELEMENT_VISIBLE", "loginButton", evidence.targetPage(), "pom-scope-sanitizer:login-button", 0.95d));
        }
        if (hasLocator(locators, "usernameInput") && hasLocator(locators, "passwordInput") && hasLocator(locators, "loginButton")) {
            add(assertions, new PromptReadyAssertion("FORM_VISIBLE", "loginForm", evidence.targetPage(), "pom-scope-sanitizer:login-form", 0.95d));
        }
    }

    private void addAuthenticatedAreaAssertions(
            Map<String, PromptReadyAssertion> assertions,
            ua.demo.agentlab.ai.context.PromptUiEvidence evidence,
            List<PromptReadyLocator> locators
    ) {
        String route = evidence.targetRoute();
        if (!route.isBlank()) {
            add(assertions, new PromptReadyAssertion("URL_CONTAINS", route, evidence.targetPage(), "pom-scope-sanitizer:authenticated-route", 1.0d));
        }
        if (hasLocator(locators, "dashboardHeading")) {
            add(assertions, new PromptReadyAssertion("ELEMENT_VISIBLE", "dashboardHeading", evidence.targetPage(), "pom-scope-sanitizer:dashboard-heading", 0.95d));
        }
        // Logout is not a generic dashboard assertion. It is emitted only from page-owned
        // requirement evidence, where the user-menu prerequisite can also be validated.
    }

    private boolean hasLogoutMenuRequirement(AiContextPackage context, Set<String> scopedIds, String targetPage) {
        if (context == null || context.uiTestPlan() == null) {
            return false;
        }
        return context.uiTestPlan().scenarios().stream()
                .filter(scenario -> scopedIds == null || scopedIds.isEmpty() || scopedIds.contains(scenario.id()))
                .filter(scenario -> belongsToRequestedPage(scenario.pageName(), targetPage)
                        || belongsToRequestedPage(scenario.sourcePageName(), targetPage))
                .map(scenario -> String.join(" ",
                        safe(scenario.title()),
                        String.join(" ", scenario.actions()),
                        String.join(" ", scenario.assertions())))
                .map(this::normalize)
                .anyMatch(text -> containsAny(text, "logout", "sign out") && containsAny(text, "user menu", "menu"));
    }

    private boolean hasLogoutMenuEvidence(
            AiContextPackage context,
            Set<String> scopedIds,
            String targetPage,
            Set<String> actions
    ) {
        if (hasLogoutMenuRequirement(context, scopedIds, targetPage)) {
            return true;
        }
        if (actions != null && actions.stream().map(this::normalize)
                .anyMatch(action -> containsAny(action, "openusermenu", "open user menu", "logout"))) {
            return true;
        }
        if (context == null || context.promptUiEvidence() == null) {
            return false;
        }
        return context.promptUiEvidence().requiredAssertions().stream()
                .filter(assertion -> belongsToRequestedPage(assertion.ownerPage(), targetPage))
                .filter(assertion -> belongsToScoped(assertion.sourceTrace(), scopedIds))
                .map(assertion -> normalize(assertion.type() + " " + assertion.expectedValue()))
                .anyMatch(text -> containsAny(text, "logout", "sign out") && containsAny(text, "user menu", "menu", "link"));
    }

    private void add(Map<String, PromptReadyAssertion> assertions, PromptReadyAssertion assertion) {
        assertions.putIfAbsent(assertion.type() + "|" + assertion.expectedValue(), assertion);
    }

    private PromptReadyAssertion elementAssertion(
            String locatorId,
            String targetPage,
            PromptAssertionEvidence assertion,
            List<PromptReadyLocator> locators
    ) {
        if (!hasLocator(locators, locatorId)) {
            return null;
        }
        return new PromptReadyAssertion("ELEMENT_VISIBLE", locatorId, targetPage, assertion.sourceTrace(), assertion.confidence());
    }

    private List<PromptReadyLocator> canonicalLocators(List<PromptLocatorEvidence> source, String targetPage, String targetRoute) {
        Map<String, PromptReadyLocator> bestById = new LinkedHashMap<>();
        for (PromptLocatorEvidence locator : source == null ? List.<PromptLocatorEvidence>of() : source) {
            if (locator.evidenceType() != LocatorEvidenceType.CONFIRMED_LOCATOR
                    || !locator.sameOrigin()
                    || locator.stabilityScore() < 0.75d
                    || locator.value().isBlank()) {
                continue;
            }
            String id = locatorIdResolver.resolve(locator);
            if (isAuthenticationPage(targetPage, targetRoute) && id.equals("submitButton")) {
                id = "loginButton";
            }
            if (id.equals("userMenuTrigger") && isDropdownMenuItemLocator(locator)) {
                continue;
            }
            PromptReadyLocator ready = new PromptReadyLocator(
                    id,
                    firstNonBlank(locator.elementName(), id),
                    locator.strategy(),
                    locator.value(),
                    locator.role(),
                    locator.componentName(),
                    locator.componentType(),
                    locator.sameOrigin(),
                    locator.uniqueWithinComponent(),
                    locator.globalMatchCount(),
                    locator.scopedMatchCount(),
                    locator.stabilityScore(),
                    locator.evidenceType(),
                    locator.sourceTrace()
            );
            PromptReadyLocator existing = bestById.get(id);
            if (existing == null || compareLocator(ready, existing) > 0) {
                bestById.put(id, ready);
            }
        }
        return bestById.values().stream()
                .sorted(Comparator.comparing(PromptReadyLocator::id))
                .toList();
    }

    /** Keeps only locators that can implement the typed action/assertion contract for this page. */
    private List<PromptReadyLocator> selectContractLocators(
            List<PromptReadyLocator> locators,
            List<String> actions,
            List<PromptReadyAssertion> assertions,
            String targetPage,
            String targetRoute
    ) {
        Set<String> required = new LinkedHashSet<>();
        for (String action : actions == null ? List.<String>of() : actions) {
            String normalized = normalize(action);
            if (containsAny(normalized, "username")) required.add("usernameInput");
            if (containsAny(normalized, "password")) required.add("passwordInput");
            if (containsAny(normalized, "login", "submit")) required.add("loginButton");
            if (containsAny(normalized, "openusermenu", "user menu")) required.add("userMenuTrigger");
            if (containsAny(normalized, "logout", "signout")) required.add("logoutLink");
            if (containsAny(normalized, "search")) required.add("searchInput");
        }
        for (PromptReadyAssertion assertion : assertions == null ? List.<PromptReadyAssertion>of() : assertions) {
            if (assertion != null && !assertion.expectedValue().isBlank()) {
                required.add(assertion.expectedValue());
            }
        }
        if (isAuthenticationPage(targetPage, targetRoute)) {
            required.add("usernameInput");
            required.add("passwordInput");
            required.add("loginButton");
        }
        return (locators == null ? List.<PromptReadyLocator>of() : locators).stream()
                .filter(locator -> required.contains(locator.id()))
                .sorted(Comparator.comparing(PromptReadyLocator::id))
                .toList();
    }

    private boolean isDropdownMenuItemLocator(PromptLocatorEvidence locator) {
        String value = normalize(locator.value());
        return containsAny(value,
                "oxd-userdropdown-link",
                "a[href",
                "href=",
                "auth/logout",
                "help/support",
                "updatepassword");
    }

    private int compareLocator(PromptReadyLocator left, PromptReadyLocator right) {
        int score = Double.compare(left.score() + strategyBonus(left.strategy()), right.score() + strategyBonus(right.strategy()));
        if (score != 0) {
            return score;
        }
        return Integer.compare(right.value().length(), left.value().length());
    }

    private double strategyBonus(String strategy) {
        String normalized = normalize(strategy);
        if (normalized.equals("id")) {
            return 0.05d;
        }
        if (normalized.equals("name")) {
            return 0.04d;
        }
        if (normalized.equals("css")) {
            return 0.02d;
        }
        if (normalized.equals("xpath")) {
            return -0.10d;
        }
        return 0.0d;
    }

    private boolean isNoisyAction(String normalized) {
        return containsAny(normalized, "orangehrminc", "orangehrm inc", "external")
                || normalized.equals("link:click")
                || normalized.startsWith("clicklink")
                || normalized.contains("forgotyourpassword");
    }

    private boolean isPostLoginAssertion(String expectedValue) {
        String normalized = normalize(expectedValue);
        return containsAny(normalized,
                "authenticated area",
                "successful login state",
                "logged with valid credentials",
                "welcome message",
                "logout action"
        );
    }

    private boolean isAuthenticatedAreaPage(String targetPage, String targetRoute) {
        String evidence = normalize(targetPage + " " + targetRoute);
        return containsAny(evidence, "dashboard", "authenticated", "secure");
    }

    private boolean looksLikeRequirementSentence(String expectedValue) {
        String normalized = normalize(expectedValue);
        return containsAny(normalized,
                " route contains ",
                " route matches ",
                " field is visible",
                " button is visible",
                " page is accessible",
                " user can ",
                "user can ",
                "valid username",
                "valid password",
                "provided by the configured",
                "performed only on",
                "belongs to",
                "owns ",
                "must ",
                "should ",
                "not expose",
                "not become",
                "not acceptable",
                "used only as",
                "stable in repeated",
                "prefer stable",
                "xpath locators",
                " generated ",
                "generated "
        );
    }

    private boolean isKnownLocatorExpectedValue(String expectedValue, List<PromptReadyLocator> locators) {
        String value = expectedValue == null ? "" : expectedValue.trim();
        return locators.stream().anyMatch(locator -> locator.id().equals(value));
    }

    private String routeFromAssertion(String expectedValue) {
        String value = expectedValue == null ? "" : expectedValue.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(/[A-Za-z0-9._~!$&'()*+,;=:@%/-]+)").matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private boolean hasLocator(List<PromptReadyLocator> locators, String id) {
        return locators.stream().anyMatch(locator -> locator.id().equals(id));
    }

    private Set<String> scopedIds(List<UiTestScenario> pageScenarios) {
        Set<String> ids = new LinkedHashSet<>();
        if (pageScenarios != null) {
            pageScenarios.stream()
                    .map(UiTestScenario::id)
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(ids::add);
        }
        return ids;
    }

    private boolean belongsToScoped(String sourceTrace, Set<String> scopedIds) {
        if (scopedIds == null || scopedIds.isEmpty()) {
            return true;
        }
        String source = sourceTrace == null ? "" : sourceTrace;
        if (!source.contains("REQ-")) {
            return true;
        }
        return scopedIds.stream().anyMatch(source::contains);
    }

    private boolean belongsToRequestedPage(String ownerPage, String requestedPageName) {
        if (ownerPage == null || ownerPage.isBlank() || requestedPageName == null || requestedPageName.isBlank()) {
            return false;
        }
        return PageReferenceMatcher.matchesScenarioPage(ownerPage, "", requestedPageName);
    }

    private boolean isAuthenticationPage(String pageName, String targetRoute) {
        String evidence = normalize(pageName + " " + targetRoute);
        return containsAny(evidence, "login", "authentication", "auth/login", "signin", "sign-in");
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

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
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
}
