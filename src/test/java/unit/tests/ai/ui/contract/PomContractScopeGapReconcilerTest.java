package unit.tests.ai.ui.contract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.PomContractScopeGapReconciler;
import ua.demo.agentlab.ai.ui.contract.PomContractScopeValidator;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

import java.util.List;

public class PomContractScopeGapReconcilerTest {

    @Test
    public void retainsRequiredMappedCoverageGapWhenLlmOmitsIt() {
        String requiredGap = "Logout user-menu flow requires confirmed userMenuTrigger and logoutLink; missing confirmed userMenuTrigger.";
        PromptReadyPomScope scope = new PromptReadyPomScope(
                "DashboardPage", "/dashboard", true, List.of("LoginPage"), List.of("REQ-LOGOUT"),
                List.of(), List.of(), List.of(), List.of(requiredGap), List.of(), 1.0d
        );
        PomContractSpec llmContract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard", "AUTHENTICATED_AREA", "openDashboard"),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );

        PomContractSpec reconciled = new PomContractScopeGapReconciler().reconcile(llmContract, scope);

        Assert.assertEquals(reconciled.coverageGaps(), List.of(requiredGap));
        Assert.assertFalse(new PomContractScopeValidator().validate(reconciled, scope).hasBlockingIssues());
    }

    @Test
    public void preservesLlmCoverageGapsWithoutDuplicates() {
        PromptReadyPomScope scope = new PromptReadyPomScope(
                "LoginPage", "/login", false, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of("Missing stable submit locator"), List.of(), 1.0d
        );
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION", "openLogin"),
                List.of(), List.of(), List.of(),
                List.of("Existing gap", "Missing stable submit locator"), List.of()
        );

        PomContractSpec reconciled = new PomContractScopeGapReconciler().reconcile(contract, scope);

        Assert.assertEquals(reconciled.coverageGaps(), List.of("Existing gap", "Missing stable submit locator"));
    }
}
