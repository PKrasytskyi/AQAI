package unit.tests.ai.ui.prompt.scope;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.prompt.scope.PomScopeSanitizer;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

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
}
