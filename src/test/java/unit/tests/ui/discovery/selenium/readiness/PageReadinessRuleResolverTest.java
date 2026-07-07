package unit.tests.ui.discovery.selenium.readiness;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRule;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRuleResolver;

import java.util.Properties;

public class PageReadinessRuleResolverTest {

    @Test
    public void resolvesAuthenticationReadinessFromProfileRoute() {
        PageReadinessRule rule = new PageReadinessRuleResolver(new ConfirmedPageSourceResolver(), new Properties())
                .resolve(profile(), null, "https://example.test/web/index.php/auth/login");

        Assert.assertEquals(rule.capability(), PageCapability.AUTHENTICATION);
        Assert.assertEquals(rule.route(), "/auth/login");
        Assert.assertTrue(rule.requiredCssSelectors().stream().anyMatch(selector -> selector.contains("password")));
        Assert.assertTrue(rule.requiredCssSelectors().stream().anyMatch(selector -> selector.contains("submit")));
    }

    @Test
    public void appliesCapabilityOverridesFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("ui.discovery.readiness.dashboard.required-css", "[data-testid='dashboard']");
        properties.setProperty("ui.discovery.readiness.dashboard.ready-text", "dashboard;profile");
        properties.setProperty("ui.discovery.readiness.dashboard.timeout-ms", "15000");

        PageReadinessRule rule = new PageReadinessRuleResolver(new ConfirmedPageSourceResolver(), properties)
                .resolve(profile(), null, "https://example.test/web/index.php/dashboard/index");

        Assert.assertEquals(rule.capability(), PageCapability.DASHBOARD);
        Assert.assertEquals(rule.requiredCssSelectors(), java.util.List.of("[data-testid='dashboard']"));
        Assert.assertEquals(rule.readyTextFragments(), java.util.List.of("dashboard", "profile"));
        Assert.assertEquals(rule.timeoutMillis(), 15000L);
        Assert.assertTrue(rule.source().contains("profile-override"));
    }

    @Test
    public void fallsBackToGenericDomRuleForUnknownNavigationRoute() {
        PageReadinessRule rule = new PageReadinessRuleResolver(new ConfirmedPageSourceResolver(), new Properties())
                .resolve(profile(), null, "https://example.test/web/index.php/help");

        Assert.assertEquals(rule.capability(), PageCapability.NAVIGATION);
        Assert.assertTrue(rule.requiredCssSelectors().isEmpty());
    }

    private ProjectProfile profile() {
        return new ProjectProfile(
                "test",
                "Test App",
                "https://example.test/web/index.php",
                "/auth/login",
                "/auth/login",
                "",
                "/dashboard/index",
                "",
                "",
                "/auth/login",
                "/dashboard/index",
                "",
                "",
                "",
                new OutputProfile("generated.pages", "generated.tests")
        );
    }
}
