package unit.tests.ai.ui.contract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractScopeValidator;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyAssertion;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyLocator;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public class PomContractScopeValidatorTest {

    @Test
    public void rejectsActionThatIsNotOwnedByThePromptScope() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard", "DASHBOARD", "openDashboard"),
                List.of(contractLocator("userMenuTrigger")),
                List.of(),
                List.of(new PomActionSpec("openDashboard", List.of(), List.of(
                        new PomStepSpec(PomStepAction.OPEN_ROUTE, "", "", "", "/dashboard")
                ))),
                List.of(),
                List.of(),
                List.of()
        );

        var report = new PomContractScopeValidator().validate(contract, dashboardScope());

        Assert.assertTrue(report.hasBlockingIssues());
        Assert.assertTrue(report.issues().stream().anyMatch(issue -> issue.ruleId().equals("POM_SCOPE_ACTION_DECLARED")));
    }

    @Test
    public void rejectsIrrelevantLocatorEvenWhenItIsConfirmedElsewhereOnThePage() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard", "DASHBOARD", "openDashboard"),
                List.of(contractLocator("userMenuTrigger"), contractLocator("adminLink")),
                List.of(),
                List.of(new PomActionSpec("openUserMenu", List.of(), List.of(
                        new PomStepSpec(PomStepAction.CLICK, "userMenuTrigger", "", "", "")
                ))),
                List.of(),
                List.of("logoutLink assertion is not available"),
                List.of()
        );

        var report = new PomContractScopeValidator().validate(contract, dashboardScope());

        Assert.assertTrue(report.issues().stream().anyMatch(issue -> issue.ruleId().equals("POM_SCOPE_LOCATOR_ALLOWED")
                && issue.evidence().equals("adminLink")));
    }

    @Test
    public void requiresDeclaredScopeCoverageGapToBeRetainedByContract() {
        PromptReadyPomScope scope = new PromptReadyPomScope(
                "DashboardPage", "/dashboard", true, List.of("LoginPage"), List.of("REQ-LOGOUT"),
                List.of(),
                List.of(),
                List.of(),
                List.of("Logout user-menu flow requires confirmed userMenuTrigger and logoutLink; missing confirmed userMenuTrigger."),
                List.of(),
                1.0d
        );
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard", "DASHBOARD", "openDashboard"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        var report = new PomContractScopeValidator().validate(contract, scope);

        Assert.assertTrue(report.hasBlockingIssues());
        Assert.assertTrue(report.issues().stream()
                .anyMatch(issue -> issue.ruleId().equals("POM_SCOPE_COVERAGE_GAP_REPRESENTED")));
    }

    @Test
    public void matchesElementVisibilityByBoundLocatorInsteadOfBusinessExpectedText() {
        PromptReadyPomScope scope = new PromptReadyPomScope(
                "LoginPage", "/login", false, List.of(), List.of("REQ-LOGIN"),
                List.of(),
                List.of(new PromptReadyAssertion("ELEMENT_VISIBLE", "Username input is visible and enabled",
                        "LoginPage", "username", "REQ-LOGIN", 1.0d)),
                List.of(locator("username")), List.of(), 1.0d
        );
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION", "openLogin"),
                List.of(contractLocator("username")),
                List.of(),
                List.of(new ua.demo.agentlab.ai.ui.contract.PomAssertionSpec(
                        "isUsernameVisible", "boolean",
                        List.of(new PomCheckSpec(PomCheckType.VISIBLE, "username", "", "", "", "")),
                        "AND")),
                List.of(),
                List.of()
        );

        var report = new PomContractScopeValidator().validate(contract, scope);

        Assert.assertFalse(report.issues().stream()
                .anyMatch(issue -> issue.ruleId().equals("POM_SCOPE_ASSERTION_COVERED")));
    }

    private PromptReadyPomScope dashboardScope() {
        return new PromptReadyPomScope(
                "DashboardPage", "/dashboard", true, List.of("LoginPage"), List.of("REQ-1"),
                List.of("openUserMenu()"),
                List.of(new PromptReadyAssertion("ELEMENT_VISIBLE", "logoutLink", "DashboardPage", "REQ-1", 1.0d)),
                List.of(locator("userMenuTrigger"), locator("logoutLink")), List.of(), 1.0d
        );
    }

    private PromptReadyLocator locator(String id) {
        return new PromptReadyLocator(id, id, "css", "#" + id, "button", "NavigationComponent", "NAVIGATION",
                true, true, 1, 1, 0.90d, LocatorEvidenceType.CONFIRMED_LOCATOR, List.of("fixture"));
    }

    private PomLocatorSpec contractLocator(String id) {
        return new PomLocatorSpec(id, id, "css", "#" + id, "button", 0.90d,
                "CONFIRMED_LOCATOR", true, true, List.of("fixture"));
    }
}
