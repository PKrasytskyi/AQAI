package unit.tests.ai.ui.prompt;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.prompt.PageObjectCapabilityContractFormatter;

import java.util.List;

public class PageObjectCapabilityContractFormatterTest {

    @Test
    public void semanticAuthenticationEvidenceBuildsPomFriendlyOwnedActions() {
        PromptUiEvidence evidence = new PromptUiEvidence(
                "LoginPage",
                "/login",
                List.of("REQ-LOGIN"),
                List.of(
                        new PromptActionEvidence("AUTHENTICATION", "semantic-business-intent", "LoginPage", "semantic-page:login"),
                        new PromptActionEvidence("username:TYPE", "semantic-action", "LoginPage", "semantic-action:username"),
                        new PromptActionEvidence("password:TYPE", "semantic-action", "LoginPage", "semantic-action:password"),
                        new PromptActionEvidence("loginButton:CLICK", "semantic-action", "LoginPage", "semantic-action:login-button")
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
        AiPageObjectSpec baseline = new AiPageObjectSpec("LoginPage", "/login", "openLogin", List.of(), List.of());

        String contract = new PageObjectCapabilityContractFormatter().format(context, "LoginPage", baseline);

        Assert.assertTrue(contract.contains("enterUsername(String username)"));
        Assert.assertTrue(contract.contains("enterPassword(String password)"));
        Assert.assertTrue(contract.contains("clickLoginButton()"));
        Assert.assertTrue(contract.contains("login(String username, String password)"));
        Assert.assertFalse(contract.contains("Authenticate using the configured credentials"));
    }

    @Test
    public void protectedPageDoesNotOwnAuthenticationActionsEvenWhenSemanticEvidenceLeaks() {
        PromptUiEvidence evidence = new PromptUiEvidence(
                "DashboardPage",
                "/dashboard/index",
                List.of("REQ-DASHBOARD"),
                List.of(
                        new PromptActionEvidence("AUTHENTICATION", "semantic-business-intent", "DashboardPage", "semantic-page:dashboard"),
                        new PromptActionEvidence("username:TYPE", "semantic-action", "DashboardPage", "semantic-action:username"),
                        new PromptActionEvidence("loginButton:CLICK", "semantic-action", "DashboardPage", "semantic-action:login-button")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("test"),
                0.80d
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
        AiPageObjectSpec baseline = new AiPageObjectSpec(
                "DashboardPage",
                "/dashboard/index",
                "openDashboard",
                List.of(),
                List.of()
        );

        String contract = new PageObjectCapabilityContractFormatter().format(context, "DashboardPage", baseline);

        Assert.assertFalse(contract.contains("enterUsername(String username)"));
        Assert.assertFalse(contract.contains("enterPassword(String password)"));
        Assert.assertFalse(contract.contains("clickLoginButton()"));
        Assert.assertFalse(contract.contains("login(String username, String password)"));
        Assert.assertTrue(contract.contains("prerequisitePages=[LoginPage]"));
        Assert.assertTrue(contract.contains("forbiddenMethods=[login, enterUsername, enterPassword, submitLogin"));
    }
}
