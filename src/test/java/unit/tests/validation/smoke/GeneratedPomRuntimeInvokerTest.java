package unit.tests.validation.smoke;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.validation.smoke.GeneratedPomRuntimeInvoker;

public class GeneratedPomRuntimeInvokerTest {

    private final GeneratedPomRuntimeInvoker invoker = new GeneratedPomRuntimeInvoker();

    @Test
    public void executesAuthenticationAndLogoutThroughGeneratedPomApi() {
        FakeLoginPage login = new FakeLoginPage();
        FakeDashboardPage dashboard = new FakeDashboardPage();

        invoker.open(login);
        invoker.authenticate(login, "demo-user", "demo-password");
        invoker.openUserMenu(dashboard);
        boolean visible = invoker.logoutVisible(dashboard);
        invoker.logout(dashboard);

        Assert.assertTrue(login.opened);
        Assert.assertEquals(login.username, "demo-user");
        Assert.assertEquals(login.password, "demo-password");
        Assert.assertTrue(login.submitted);
        Assert.assertTrue(dashboard.menuOpened);
        Assert.assertTrue(visible);
        Assert.assertTrue(dashboard.loggedOut);
    }

    @Test
    public void invokesPageSpecificRouteAssertions() {
        Assert.assertTrue(invoker.routeMatches(new FakeLoginPage(), "/auth/login"));
        Assert.assertTrue(invoker.routeMatches(new FakeDashboardPage(), "/dashboard/index"));
    }

    public static final class FakeLoginPage {
        private boolean opened;
        private boolean submitted;
        private String username;
        private String password;

        public void openLogin() { opened = true; }
        public void enterUsername(String value) { username = value; }
        public void enterPassword(String value) { password = value; }
        public void clickLoginButton() { submitted = true; }
        public boolean urlContainsAuthLogin() { return true; }
    }

    public static final class FakeDashboardPage {
        private boolean menuOpened;
        private boolean loggedOut;

        public void openUserMenu() { menuOpened = true; }
        public boolean elementVisibleLogout() { return menuOpened; }
        public void logout() { loggedOut = true; }
        public boolean urlContainsDashboardIndex() { return true; }
    }
}
