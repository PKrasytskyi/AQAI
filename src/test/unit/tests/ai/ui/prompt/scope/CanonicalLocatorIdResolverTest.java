package unit.tests.ai.ui.prompt.scope;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.ui.prompt.scope.CanonicalLocatorIdResolver;

import java.util.List;

public class CanonicalLocatorIdResolverTest {

    private final CanonicalLocatorIdResolver resolver = new CanonicalLocatorIdResolver();

    @Test
    public void resolvesLogoutBeforeSubmitFallback() {
        String id = resolver.resolve(locator(
                "logoutControl",
                "logout control",
                "button",
                "a[href*='logout'], button[type='submit']",
                "Logout"
        ));

        Assert.assertEquals(id, "logoutLink");
    }

    @Test
    public void resolvesBareSubmitAsNeutralSubmitButton() {
        String id = resolver.resolve(locator(
                "submit",
                "submit",
                "button",
                "button[type='submit']",
                "Submit"
        ));

        Assert.assertEquals(id, "submitButton");
    }

    @Test
    public void resolvesLoginEvidenceAsLoginButton() {
        String id = resolver.resolve(locator(
                "loginButton",
                "login button",
                "button",
                "button[type='submit']",
                "Login"
        ));

        Assert.assertEquals(id, "loginButton");
    }

    private PromptLocatorEvidence locator(
            String fieldHint,
            String elementName,
            String role,
            String value,
            String visibleText
    ) {
        return new PromptLocatorEvidence(
                fieldHint,
                elementName,
                "css",
                value,
                role,
                visibleText,
                "",
                true,
                0.88d,
                List.of("test")
        );
    }
}
