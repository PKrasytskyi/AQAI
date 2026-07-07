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

    @Test
    public void resolvesDashboardHeadingEvidence() {
        String id = resolver.resolve(locator(
                "dashboard heading",
                "dashboard heading",
                "heading",
                "h6.oxd-topbar-header-breadcrumb-module",
                "Dashboard"
        ));

        Assert.assertEquals(id, "dashboardHeading");
    }

    @Test
    public void resolvesUserDropdownTriggerEvidence() {
        String id = resolver.resolve(locator(
                "user dropdown tab",
                "user dropdown tab",
                "button",
                "span.oxd-userdropdown-tab",
                "Yazeed Ali"
        ));

        Assert.assertEquals(id, "userMenuTrigger");
    }

    @Test
    public void resolvesChangePasswordLinkWithoutTreatingItAsPasswordInput() {
        String id = resolver.resolve(locator(
                "password",
                "changePassword",
                "link",
                "a[href='/web/index.php/pim/updatePassword']",
                "Change Password"
        ));

        Assert.assertEquals(id, "changePasswordLink");
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
