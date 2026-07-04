package unit.tests.ai.ui.prompt.quality;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.model.AiLocatorSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityLinter;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityReport;

import java.util.List;

public class PromptQualityLinterTest {

    private final PromptQualityLinter linter = new PromptQualityLinter();

    @Test
    public void normalPasswordLocatorIsNotTreatedAsStale() {
        AiPageObjectSpec baseline = new AiPageObjectSpec(
                "LoginPage",
                "/auth/login",
                "openLogin",
                List.of(
                        new AiLocatorSpec("usernameInput", "username input", "name", "username"),
                        new AiLocatorSpec("passwordInput", "password input", "name", "password"),
                        new AiLocatorSpec("loginButton", "login button", "css", "button[type='submit']")
                ),
                List.of()
        );

        PromptQualityReport report = linter.validate(
                minimalPrompt(),
                "LoginPage",
                null,
                List.of(),
                baseline
        );

        Assert.assertFalse(hasRule(report, "NO_STALE_LOCATOR_CANDIDATES"));
    }

    @Test
    public void passwordLocatorUsingLoginIdIsTreatedAsStale() {
        AiPageObjectSpec baseline = new AiPageObjectSpec(
                "LoginPage",
                "/auth/login",
                "openLogin",
                List.of(new AiLocatorSpec("passwordInput", "password input", "id", "login")),
                List.of()
        );

        PromptQualityReport report = linter.validate(
                minimalPrompt(),
                "LoginPage",
                null,
                List.of(),
                baseline
        );

        Assert.assertTrue(hasRule(report, "NO_STALE_LOCATOR_CANDIDATES"));
    }

    private boolean hasRule(PromptQualityReport report, String ruleId) {
        return report.issues().stream().anyMatch(issue -> ruleId.equals(issue.ruleId()));
    }

    private String minimalPrompt() {
        return """
                # Goal
                pageName=LoginPage
                route=/auth/login
                Defined test cases:
                expectedValue=Login page is visible
                requiredLocators=[]
                forbiddenMethods=[]
                Baseline page object spec:
                """;
    }
}
