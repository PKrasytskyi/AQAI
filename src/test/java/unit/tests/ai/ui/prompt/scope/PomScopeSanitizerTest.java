package unit.tests.ai.ui.prompt.scope;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.prompt.scope.PomScopeSanitizer;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public class PomScopeSanitizerTest {

    @Test
    public void dropsCrossRouteUrlAssertionFromLoginPageScope() {
        PromptUiEvidence evidence = new PromptUiEvidence(
                "LoginPage",
                "/auth/login",
                List.of("REQ-LOGIN"),
                List.of(),
                List.of(
                        new PromptAssertionEvidence("URL_CONTAINS", "/dashboard/index", "LoginPage", "REQ-LOGIN", 1.0d),
                        new PromptAssertionEvidence("URL_CONTAINS", "/auth/login", "LoginPage", "REQ-LOGIN", 1.0d)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of("test"),
                0.90d
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                evidence
        );

        PromptReadyPomScope scope = new PomScopeSanitizer().sanitize(context, "LoginPage", List.of());

        Assert.assertTrue(scope.ownedAssertions().stream()
                .anyMatch(assertion -> assertion.type().equals("URL_CONTAINS")
                        && assertion.expectedValue().equals("/auth/login")));
        Assert.assertTrue(scope.ownedAssertions().stream()
                .noneMatch(assertion -> assertion.expectedValue().equals("/dashboard/index")));
        Assert.assertTrue(scope.rejectedSuggestions().stream()
                .anyMatch(value -> value.contains("target-after-navigation")));
    }

    @Test
    public void dashboardLogoutScopeRequiresUserMenuBeforeLogout() {
        PromptUiEvidence evidence = new PromptUiEvidence(
                "DashboardPage",
                "/dashboard/index",
                true,
                List.of("LoginPage"),
                List.of("REQ-LOGOUT"),
                List.of(new PromptActionEvidence(
                        "userMenuTrigger:CLICK",
                        "semantic-action",
                        "DashboardPage",
                        "semantic-action:web-index-php-dashboard-index:element:user-menu-trigger"
                )),
                List.of(new PromptAssertionEvidence(
                        "ELEMENT_VISIBLE",
                        "logoutLink",
                        "DashboardPage",
                        "REQ-LOGOUT",
                        0.90d
                )),
                List.of(
                        new PromptLocatorEvidence(
                                "userMenuTrigger",
                                "user menu trigger",
                                "css",
                                "span.oxd-userdropdown-tab",
                                "button",
                                "Admin",
                                "",
                                true,
                                0.78d,
                                "NavigationComponent",
                                "NAVIGATION",
                                1,
                                1,
                                true,
                                LocatorEvidenceType.CONFIRMED_LOCATOR,
                                List.of("dependency-locator:web-index-php-dashboard-index:element:user-menu-trigger")
                        ),
                        new PromptLocatorEvidence(
                                "logoutLink",
                                "logout",
                                "css",
                                "a[href='/web/index.php/auth/logout']",
                                "link",
                                "Logout",
                                "/web/index.php/auth/logout",
                                true,
                                0.87d,
                                "NavigationComponent",
                                "NAVIGATION",
                                1,
                                1,
                                true,
                                LocatorEvidenceType.CONFIRMED_LOCATOR,
                                List.of("ranked-evidence:web-index-php-dashboard-index:element:logout")
                        )
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("test"),
                0.90d
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                evidence
        );

        PromptReadyPomScope scope = new PomScopeSanitizer().sanitize(context, "DashboardPage", List.of());

        Assert.assertEquals(scope.ownedActions(), List.of("openUserMenu()", "logout()"));
        Assert.assertTrue(scope.allowedLocators().stream().anyMatch(locator -> locator.id().equals("userMenuTrigger")));
        Assert.assertTrue(scope.allowedLocators().stream().anyMatch(locator -> locator.id().equals("logoutLink")));
    }

    @Test
    public void rejectsLogoutActionWhenRawEvidenceIncorrectlyAssignsItToLoginPage() {
        PromptUiEvidence evidence = new PromptUiEvidence(
                "LoginPage",
                "/auth/login",
                false,
                List.of(),
                List.of("REQ-LOGIN"),
                List.of(new PromptActionEvidence("logout:CLICK", "semantic-action", "LoginPage", "REQ-LOGIN")),
                List.of(),
                List.of(
                        new PromptLocatorEvidence("usernameInput", "username", "name", "username", "input", "", "",
                                true, 0.90d, "LoginForm", "FORM", 1, 1, true,
                                LocatorEvidenceType.CONFIRMED_LOCATOR, List.of("fixture")),
                        new PromptLocatorEvidence("passwordInput", "password", "name", "password", "password", "", "",
                                true, 0.90d, "LoginForm", "FORM", 1, 1, true,
                                LocatorEvidenceType.CONFIRMED_LOCATOR, List.of("fixture")),
                        new PromptLocatorEvidence("loginButton", "login", "css", "button[type='submit']", "button", "", "",
                                true, 0.90d, "LoginForm", "FORM", 1, 1, true,
                                LocatorEvidenceType.CONFIRMED_LOCATOR, List.of("fixture"))
                ),
                List.of(), List.of(), List.of(), List.of(), List.of("test"), 0.90d
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), evidence
        );

        PromptReadyPomScope scope = new PomScopeSanitizer().sanitize(context, "LoginPage", List.of());

        Assert.assertTrue(scope.ownedActions().stream().noneMatch(action -> action.toLowerCase().contains("logout")));
        Assert.assertTrue(scope.rejectedSuggestions().stream().anyMatch(value -> value.contains("authenticated-area page")));
    }

    @Test
    public void emitsCoverageGapInsteadOfAnUnsafeLogoutAssertionWhenMenuTriggerIsNotConfirmed() {
        PromptUiEvidence evidence = new PromptUiEvidence(
                "DashboardPage",
                "/dashboard/index",
                true,
                List.of("LoginPage"),
                List.of("REQ-LOGOUT"),
                List.of(),
                List.of(new PromptAssertionEvidence(
                        "ELEMENT_VISIBLE",
                        "logoutLink",
                        "DashboardPage",
                        "REQ-LOGOUT",
                        0.90d
                )),
                List.of(new PromptLocatorEvidence(
                        "logoutLink",
                        "logout",
                        "css",
                        "a[href='/web/index.php/auth/logout']",
                        "link",
                        "Logout",
                        "/web/index.php/auth/logout",
                        true,
                        0.87d,
                        "NavigationComponent",
                        "NAVIGATION",
                        1,
                        1,
                        true,
                        LocatorEvidenceType.CONFIRMED_LOCATOR,
                        List.of("fixture")
                )),
                List.of(), List.of(), List.of(), List.of(), List.of("test"), 0.90d
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), evidence
        );

        PromptReadyPomScope scope = new PomScopeSanitizer().sanitize(context, "DashboardPage", List.of());

        Assert.assertTrue(scope.ownedActions().isEmpty());
        Assert.assertTrue(scope.ownedAssertions().stream()
                .noneMatch(assertion -> "logoutLink".equals(assertion.expectedValue())));
        Assert.assertTrue(scope.coverageGaps().stream()
                .anyMatch(gap -> gap.contains("userMenuTrigger") && gap.contains("logoutLink")));
    }

    @Test
    public void excludesAssertionsWithoutConfirmedPageOwnership() {
        PromptUiEvidence evidence = new PromptUiEvidence(
                "DashboardPage",
                "/dashboard/index",
                List.of("REQ-RECRUITMENT"),
                List.of(),
                List.of(new PromptAssertionEvidence(
                        "TEXT_VISIBLE",
                        "Recruitment page is opened successfully.",
                        "",
                        "REQ-RECRUITMENT",
                        0.88d
                )),
                List.of(), List.of(), List.of(), List.of("test"), 0.90d
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), evidence
        );

        PromptReadyPomScope scope = new PomScopeSanitizer().sanitize(context, "DashboardPage", List.of());

        Assert.assertTrue(scope.ownedAssertions().stream()
                .noneMatch(assertion -> assertion.expectedValue().contains("Recruitment page")));
    }

    @Test
    public void doesNotPromoteCachedLogoutLocatorToDashboardAssertionWithoutLogoutRequirement() {
        PromptUiEvidence evidence = new PromptUiEvidence(
                "DashboardPage",
                "/dashboard/index",
                true,
                List.of("LoginPage"),
                List.of("REQ-DASHBOARD"),
                List.of(),
                List.of(),
                List.of(new PromptLocatorEvidence(
                        "logoutLink", "logout", "css", "a[href='/logout']", "link", "Logout", "/logout",
                        true, 0.90d, "NavigationComponent", "NAVIGATION", 1, 1, true,
                        LocatorEvidenceType.CONFIRMED_LOCATOR, List.of("stable-cache")
                )),
                List.of(), List.of(), List.of(), List.of(), List.of("test"), 0.90d
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), evidence
        );

        PromptReadyPomScope scope = new PomScopeSanitizer().sanitize(context, "DashboardPage", List.of());

        Assert.assertTrue(scope.ownedAssertions().stream()
                .noneMatch(assertion -> "logoutLink".equals(assertion.expectedValue())));
        Assert.assertTrue(scope.allowedLocators().isEmpty());
    }
}
