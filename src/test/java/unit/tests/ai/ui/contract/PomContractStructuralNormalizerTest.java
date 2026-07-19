package unit.tests.ai.ui.contract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractStructuralNormalizer;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;

import java.util.List;

public class PomContractStructuralNormalizerTest {

    @Test
    public void removesOnlyRedundantActionThatDuplicatesPageOpenMethod() {
        PomActionSpec open = new PomActionSpec("openLogin", List.of(), List.of(
                new PomStepSpec(PomStepAction.OPEN_ROUTE, "", "", "", "/login")));
        PomActionSpec click = new PomActionSpec("clickLoginButton", List.of(), List.of(
                new PomStepSpec(PomStepAction.CLICK, "login", "", "", "")));
        PomContractSpec contract = new PomContractSpec("pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION", "openLogin"),
                List.of(), List.of(open, click), List.of(), List.of(), List.of());

        PomContractSpec normalized = new PomContractStructuralNormalizer().normalize(contract);

        Assert.assertEquals(normalized.actions().stream().map(PomActionSpec::methodName).toList(),
                List.of("clickLoginButton"));
    }

    @Test
    public void replacesRequirementSentenceOnVisibleCheckWithConcreteLocatorEvidence() {
        PomAssertionSpec assertion = new PomAssertionSpec("ELEMENT_VISIBLE_logout", "boolean", List.of(
                new PomCheckSpec(PomCheckType.VISIBLE, "logoutLink",
                        "The user menu opens and the logout action becomes visible and enabled.", "", "", "")
        ), "AND");
        PomContractSpec contract = new PomContractSpec("pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard/index", "AUTHENTICATED_AREA", "openDashboard"),
                List.of(), List.of(), List.of(assertion), List.of(), List.of());

        PomContractSpec normalized = new PomContractStructuralNormalizer().normalize(contract);

        Assert.assertEquals(normalized.assertions().get(0).checks().get(0).expectedValue(), "logoutLink");
        Assert.assertEquals(normalized.assertions().get(0).methodName(), "elementVisibleLogout");
    }

    @Test
    public void derivesRouteAssertionMethodNameFromTypedCheckInsteadOfLlmProse() {
        PomAssertionSpec assertion = new PomAssertionSpec("URL_CONTAINS_expected_/auth/login", "boolean", List.of(
                new PomCheckSpec(PomCheckType.URL_CONTAINS, "", "/auth/login", "", "", "")
        ), "AND");
        PomContractSpec contract = new PomContractSpec("pom-contract-v1",
                new PomPageSpec("LoginPage", "/auth/login", "AUTHENTICATION", "openLogin"),
                List.of(), List.of(), List.of(assertion), List.of(), List.of());

        PomContractSpec normalized = new PomContractStructuralNormalizer().normalize(contract);

        Assert.assertEquals(normalized.assertions().get(0).methodName(), "urlContainsAuthLogin");
    }
}
