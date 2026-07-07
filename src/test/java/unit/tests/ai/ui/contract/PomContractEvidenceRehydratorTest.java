package unit.tests.ai.ui.contract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.contract.PomContractEvidenceRehydrator;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyLocator;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public class PomContractEvidenceRehydratorTest {

    @Test
    public void rehydratesLocatorMetadataFromPromptReadyScope() {
        PomContractSpec contract = new PomContractSpec(
                LlmOutputSchemaVersion.POM_CONTRACT,
                new PomPageSpec("LoginPage", "/auth/login", "AUTHENTICATION", "openLogin"),
                List.of(new PomLocatorSpec("loginButton", "login", "css", "button[type='submit']", "button", 0.87d,
                        "", false, false, List.of())),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        PromptReadyPomScope scope = new PromptReadyPomScope(
                "LoginPage",
                "/auth/login",
                false,
                List.of(),
                List.of("REQ-001"),
                List.of(),
                List.of(),
                List.of(new PromptReadyLocator(
                        "loginButton",
                        "login",
                        "css",
                        "button[type='submit']",
                        "button",
                        "FormComponent",
                        "FORM",
                        true,
                        true,
                        1,
                        1,
                        0.87d,
                        LocatorEvidenceType.CONFIRMED_LOCATOR,
                        List.of("prompt-ready")
                )),
                List.of(),
                1.0d
        );

        PomContractSpec result = new PomContractEvidenceRehydrator().rehydrate(contract, scope, null);

        PomLocatorSpec locator = result.locators().get(0);
        Assert.assertEquals(locator.evidenceType(), "CONFIRMED_LOCATOR");
        Assert.assertTrue(locator.sameOrigin());
        Assert.assertTrue(locator.uniqueWithinComponent());
        Assert.assertEquals(locator.sourceTrace(), List.of("prompt-ready"));
    }
}
