package unit.tests.ai.ui.contract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractQualityGate;
import ua.demo.agentlab.ai.ui.contract.PomContractQualityReport;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;

import java.util.List;

public class PomContractQualityGateTest {

    private final PomContractQualityGate gate = new PomContractQualityGate();

    @Test
    public void acceptsValidLoginContract() {
        PomContractQualityReport report = gate.validate(loginContract());

        Assert.assertFalse(report.hasBlockingIssues(), report.issues().toString());
    }

    @Test
    public void blocksStepUsingUnknownLocator() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION_FORM", "openLogin"),
                List.of(locator("usernameInput")),
                List.of(new PomActionSpec(
                        "clickLoginButton",
                        List.of(),
                        List.of(new PomStepSpec(PomStepAction.CLICK, "missingLoginButton", "", "", ""))
                )),
                List.of(),
                List.of(),
                List.of()
        );

        PomContractQualityReport report = gate.validate(contract);

        Assert.assertTrue(report.hasBlockingIssues());
        Assert.assertTrue(report.issues().stream().anyMatch(issue -> "POM_STEP_LOCATOR_EXISTS".equals(issue.ruleId())));
    }

    private PomContractSpec loginContract() {
        return new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION_FORM", "openLogin"),
                List.of(locator("usernameInput"), locator("passwordInput"), locator("loginButton")),
                List.of(new PomActionSpec(
                        "login",
                        List.of(
                                new AiMethodParameterSpec("String", "username"),
                                new AiMethodParameterSpec("String", "password")
                        ),
                        List.of(
                                new PomStepSpec(PomStepAction.CLEAR_AND_TYPE, "usernameInput", "username", "", ""),
                                new PomStepSpec(PomStepAction.CLEAR_AND_TYPE, "passwordInput", "password", "", ""),
                                new PomStepSpec(PomStepAction.CLICK, "loginButton", "", "", "")
                        )
                )),
                List.of(new PomAssertionSpec(
                        "isLoginFormVisible",
                        "boolean",
                        List.of(
                                new PomCheckSpec(PomCheckType.VISIBLE, "usernameInput", "", "", "", ""),
                                new PomCheckSpec(PomCheckType.VISIBLE, "passwordInput", "", "", "", ""),
                                new PomCheckSpec(PomCheckType.VISIBLE, "loginButton", "", "", "", "")
                        ),
                        "AND"
                )),
                List.of(),
                List.of()
        );
    }

    private PomLocatorSpec locator(String id) {
        return new PomLocatorSpec(id, id, "id", id, "input", 0.9d);
    }
}
