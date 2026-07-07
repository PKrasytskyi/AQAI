package unit.tests.ai.context;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.AiContextScopeResolver;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.List;

public class AiContextScopeResolverTest {

    @Test
    public void protectedLogoutVisibilityScenarioBelongsToDashboardPage() {
        UiTestScenario scenario = new UiTestScenario(
                "REQ-018",
                "Logout action is visible for the authenticated user after opening the user menu",
                "flow-REQ-018",
                "VERIFY_LOGOUT_AVAILABLE",
                "LoginPage",
                "DashboardPage",
                "/auth/login",
                "/dashboard/index",
                "Application is available",
                new UiScenarioPrerequisite("LoginPage", "/auth/login", true, List.of("Authenticate using the configured credentials")),
                UiAssertionProfile.BASIC,
                List.of("Authenticate using the configured credentials"),
                List.of("Logout action is visible for the authenticated user after opening the user menu."),
                List.of(new UiOperationIntent(UiOperationKind.AUTHENTICATE, "LoginPage", null)),
                List.of(new AssertionIntent(AssertionIntentKind.CONTENT_VISIBLE,
                        "Logout action is visible for the authenticated user after opening the user menu.")),
                List.of(),
                "requirements/valid-login-requirement.md [L29]"
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM",
                null,
                null,
                null,
                null,
                null,
                new UiTestPlan("fixture", "DashboardPage", List.of("LoginPage", "DashboardPage"), List.of(scenario)),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        var scope = new AiContextScopeResolver().resolveForPage(context, "DashboardPage");

        Assert.assertTrue(scope.targetScenarioIds().contains("REQ-018"));
        Assert.assertTrue(scope.targetRoutes().contains("/dashboard/index"));
    }
}
