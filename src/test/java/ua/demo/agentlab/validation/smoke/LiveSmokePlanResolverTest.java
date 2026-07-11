package ua.demo.agentlab.validation.smoke;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public class LiveSmokePlanResolverTest {

    @Test
    public void resolvesTheInternetAuthenticationFixtureWithoutDashboardOrUserMenuAssumptions() {
        String previous = System.getProperty("project.profile.file");
        System.setProperty("project.profile.file", "profiles/the-internet.project-profile.yaml");
        try {
            GeneratedSourceFile authPage = pageObject(
                    "AuthenticationPage",
                    """
                    package pages;
                    import org.openqa.selenium.By;
                    class AuthenticationPage {
                        private final By usernameInput = By.id("username");
                        private final By passwordInput = By.id("password");
                        private final By loginButton = By.cssSelector("button[type='submit']");
                        void openAuthenticationPage() { open("/login"); }
                        void login(String username, String password) {}
                    }
                    """
            );
            GeneratedSourceFile secureArea = pageObject(
                    "SecureAreaPage",
                    """
                    package pages;
                    import org.openqa.selenium.By;
                    class SecureAreaPage {
                        private final By logoutLink = By.cssSelector("a[href='/logout']");
                        boolean routeMatchesSecureArea() { return getCurrentUrl().contains("/secure"); }
                    }
                    """
            );

            LiveSmokePlan plan = new LiveSmokePlanResolver().resolve(
                    new GeneratedUiSources(List.of(authPage, secureArea), List.of())
            );

            Assert.assertEquals(plan.sourcePage().className(), "AuthenticationPage");
            Assert.assertEquals(plan.targetPage().className(), "SecureAreaPage");
            Assert.assertEquals(plan.sourceRoute(), "/login");
            Assert.assertEquals(plan.targetRoute(), "/secure");
            Assert.assertEquals(plan.postActionRoute(), "/login");
        } finally {
            restore("project.profile.file", previous);
        }
    }

    @Test
    public void resolvesOrangeHrmAuthenticationFixtureByCapabilityEvidenceNotClassName() {
        String previous = System.getProperty("project.profile.file");
        System.setProperty("project.profile.file", "profiles/orangehrm.project-profile.yaml");
        try {
            GeneratedSourceFile source = pageObject(
                    "AuthEntryPage",
                    """
                    package pages;
                    import org.openqa.selenium.By;
                    class AuthEntryPage {
                        private final By usernameInput = By.name("username");
                        private final By passwordInput = By.name("password");
                        private final By loginButton = By.cssSelector("button[type='submit']");
                        void openAuthEntry() { open("/auth/login"); }
                        void login(String username, String password) {}
                    }
                    """
            );
            GeneratedSourceFile target = pageObject(
                    "ProtectedAreaPage",
                    """
                    package pages;
                    import org.openqa.selenium.By;
                    class ProtectedAreaPage {
                        private final By userMenuTrigger = By.cssSelector("span.oxd-userdropdown-tab");
                        private final By logoutLink = By.cssSelector("a[href='/web/index.php/auth/logout']");
                        boolean routeMatchesProtectedArea() { return getCurrentUrl().contains("/dashboard/index"); }
                    }
                    """
            );

            LiveSmokePlan plan = new LiveSmokePlanResolver().resolve(
                    new GeneratedUiSources(List.of(target, source), List.of())
            );

            Assert.assertEquals(plan.sourcePage().className(), "AuthEntryPage");
            Assert.assertEquals(plan.targetPage().className(), "ProtectedAreaPage");
            Assert.assertEquals(plan.sourceRoute(), "/auth/login");
            Assert.assertEquals(plan.targetRoute(), "/dashboard/index");
        } finally {
            restore("project.profile.file", previous);
        }
    }

    private GeneratedSourceFile pageObject(String className, String content) {
        return new GeneratedSourceFile("pages", className, "pages/" + className + ".java", content);
    }

    private void restore(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }
}
