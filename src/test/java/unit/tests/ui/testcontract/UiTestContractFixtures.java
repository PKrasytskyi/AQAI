package unit.tests.ui.testcontract;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.assertions.model.AssertionSource;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.List;

final class UiTestContractFixtures {

    private UiTestContractFixtures() {
    }

    static List<PomContractSpec> pomContracts() {
        PomContractSpec login = new PomContractSpec(
                null,
                new PomPageSpec("LoginPage", "/auth/login", "AUTHENTICATION", "openLogin"),
                List.of(
                        locator("username", "input"),
                        locator("password", "password"),
                        locator("login", "button")
                ),
                List.of(
                        action("enterUsername", "username", "username"),
                        action("enterPassword", "password", "password"),
                        click("clickLoginButton", "login")
                ),
                List.of(
                        urlAssertion("urlContainsAuthLogin", "/auth/login"),
                        visibleAssertion("elementVisibleUsername", "username"),
                        visibleAssertion("elementVisiblePassword", "password"),
                        visibleAssertion("elementVisibleLogin", "login")
                ),
                List.of(),
                List.of()
        );
        PomContractSpec dashboard = new PomContractSpec(
                null,
                new PomPageSpec("DashboardPage", "/dashboard/index", "AUTHENTICATED_AREA", "openDashboard"),
                List.of(locator("userMenu", "button"), locator("logout", "link")),
                List.of(
                        click("openUserMenu", "userMenu"),
                        new PomActionSpec(
                                "logout",
                                List.of(),
                                List.of(
                                        new PomStepSpec(PomStepAction.CLICK, "userMenu", "", "", ""),
                                        new PomStepSpec(PomStepAction.CLICK, "logout", "", "", "")
                                )
                        )
                ),
                List.of(
                        urlAssertion("urlContainsDashboardIndex", "/dashboard/index"),
                        visibleAssertion("elementVisibleLogout", "logout")
                ),
                List.of(),
                List.of()
        );
        return List.of(login, dashboard);
    }

    static CanonicalTestCaseBundle canonicalBundle() {
        CanonicalTestCase openLogin = testCase(
                "REQ-001",
                "Authentication Form Is Ready",
                "OPEN_PAGE",
                "LoginPage",
                "LoginPage",
                "/auth/login",
                "/auth/login",
                List.of(new UiOperationIntent(UiOperationKind.OPEN_PAGE, "LoginPage", null))
        );
        CanonicalTestCase authenticate = testCase(
                "REQ-002",
                "Authenticate With Valid Credentials",
                "AUTHENTICATE",
                "LoginPage",
                "DashboardPage",
                "/auth/login",
                "/dashboard/index",
                List.of(new UiOperationIntent(UiOperationKind.AUTHENTICATE, "LoginPage", null))
        );
        CanonicalTestCase openMenu = testCase(
                "REQ-003",
                "Open User Menu And Display Logout Action",
                "OPEN_MENU",
                "DashboardPage",
                "DashboardPage",
                "/dashboard/index",
                "/dashboard/index",
                List.of(
                        new UiOperationIntent(UiOperationKind.AUTHENTICATE, "LoginPage", null),
                        new UiOperationIntent(UiOperationKind.OPEN_MENU, "DashboardPage", "userMenu")
                )
        );
        CanonicalTestCase logout = testCase(
                "REQ-004",
                "Logout And Return To Authentication Page",
                "LOGOUT",
                "DashboardPage",
                "LoginPage",
                "/dashboard/index",
                "/auth/login",
                List.of(
                        new UiOperationIntent(UiOperationKind.AUTHENTICATE, "LoginPage", null),
                        new UiOperationIntent(UiOperationKind.LOGOUT, "DashboardPage", null)
                )
        );
        return new CanonicalTestCaseBundle(
                "requirements.md",
                "LoginPage",
                List.of("DashboardPage", "LoginPage"),
                List.of(openLogin, authenticate, openMenu, logout)
        );
    }

    static List<AssertionContract> assertionContracts() {
        return List.of(
                assertion("REQ-001", AssertionType.URL_CONTAINS, "/auth/login", "LoginPage", "/auth/login"),
                assertion("REQ-001", AssertionType.ELEMENT_VISIBLE, "Username input is visible", "LoginPage", "/auth/login"),
                assertion("REQ-002", AssertionType.URL_CONTAINS, "/dashboard/index", "DashboardPage", "/dashboard/index"),
                assertion("REQ-003", AssertionType.ELEMENT_VISIBLE, "Logout action is visible", "DashboardPage", "/dashboard/index"),
                assertion("REQ-004", AssertionType.URL_CONTAINS, "/auth/login", "LoginPage", "/auth/login"),
                assertion("REQ-004", AssertionType.ELEMENT_VISIBLE, "Username input is visible", "LoginPage", "/auth/login")
        );
    }

    static PomContractSpec searchPomContract() {
        return new PomContractSpec(
                null,
                new PomPageSpec("SearchPage", "/search", "SEARCH", "openSearch"),
                List.of(locator("query", "searchbox")),
                List.of(action("search", "query", "query")),
                List.of(urlAssertion("urlContainsSearch", "/search")),
                List.of(),
                List.of()
        );
    }

    static CanonicalTestCaseBundle searchCanonicalBundle() {
        return new CanonicalTestCaseBundle(
                "requirements.md",
                "SearchPage",
                List.of("SearchPage"),
                List.of(testCase(
                        "REQ-SEARCH",
                        "Search For A Record",
                        "SEARCH",
                        "SearchPage",
                        "SearchPage",
                        "/search",
                        "/search",
                        List.of(new UiOperationIntent(UiOperationKind.SEARCH, "query", "search-default"))
                ))
        );
    }

    static List<AssertionContract> searchAssertions() {
        return List.of(assertion(
                "REQ-SEARCH",
                AssertionType.URL_CONTAINS,
                "/search",
                "SearchPage",
                "/search"
        ));
    }

    private static CanonicalTestCase testCase(
            String id,
            String title,
            String flow,
            String sourcePage,
            String targetPage,
            String sourceRoute,
            String targetRoute,
            List<UiOperationIntent> operations
    ) {
        return new CanonicalTestCase(
                id,
                title,
                List.of(id),
                List.of(),
                operations,
                List.of(),
                List.of(sourcePage, targetPage),
                new UiScenarioPrerequisite(sourcePage, sourceRoute, false, List.of()),
                "flow-" + id,
                flow,
                sourcePage,
                targetPage,
                sourceRoute,
                targetRoute,
                "Application is available",
                UiAssertionProfile.BASIC,
                operations.stream().map(operation -> operation.kind().name()).toList(),
                List.of(),
                List.of(),
                "requirements.md"
        );
    }

    private static PomLocatorSpec locator(String id, String role) {
        return new PomLocatorSpec(
                id,
                id,
                "css",
                "[data-test='" + id + "']",
                role,
                0.95,
                "CONFIRMED_LOCATOR",
                true,
                true,
                List.of("unit-test")
        );
    }

    private static PomActionSpec action(String method, String locator, String parameter) {
        return new PomActionSpec(
                method,
                List.of(new AiMethodParameterSpec("String", parameter)),
                List.of(new PomStepSpec(PomStepAction.CLEAR_AND_TYPE, locator, parameter, "", ""))
        );
    }

    private static PomActionSpec click(String method, String locator) {
        return new PomActionSpec(
                method,
                List.of(),
                List.of(new PomStepSpec(PomStepAction.CLICK, locator, "", "", ""))
        );
    }

    private static PomAssertionSpec urlAssertion(String method, String route) {
        return new PomAssertionSpec(
                method,
                "boolean",
                List.of(new PomCheckSpec(PomCheckType.URL_CONTAINS, "", route, "", "", route)),
                "AND"
        );
    }

    private static PomAssertionSpec visibleAssertion(String method, String locator) {
        return new PomAssertionSpec(
                method,
                "boolean",
                List.of(new PomCheckSpec(PomCheckType.VISIBLE, locator, locator, "", "", "")),
                "AND"
        );
    }

    private static AssertionContract assertion(
            String id,
            AssertionType type,
            String expected,
            String page,
            String route
    ) {
        return new AssertionContract(
                id,
                id,
                type,
                expected,
                page,
                route,
                "requirements.md",
                1.0,
                AssertionSource.REQUIREMENT
        );
    }
}
