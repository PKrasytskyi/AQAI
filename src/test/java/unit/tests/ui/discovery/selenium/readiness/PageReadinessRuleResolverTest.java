package unit.tests.ui.discovery.selenium.readiness;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRule;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRuleResolver;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;

import java.util.List;
import java.util.Map;
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

    @Test
    public void usesStructuredTargetCapabilityInsteadOfRouteNameGuessing() {
        NormalizedRequirement requirement = new NormalizedRequirement(
                "REQ-SELECT", "Select an option", "Select option 1.", "Option 1 is selected.",
                true, false, List.of("structured-requirement", "capability-selection"),
                new SourceReference("requirements/select.md", 1, 1, ""), List.of(),
                Map.of("target context", List.of("`pageCapability: FORM_CONTROL`", "`targetRoute: /dropdown`"))
        );
        PageReadinessRule rule = new PageReadinessRuleResolver(new ConfirmedPageSourceResolver(), new Properties())
                .resolve(profile(), new NormalizedRequirementBundle(
                        "requirements/select.md", List.of(requirement), List.of(), List.of()),
                        "https://example.test/web/index.php/dropdown");

        Assert.assertEquals(rule.capability(), PageCapability.FORM);
        Assert.assertTrue(rule.requiredCssSelectors().stream().anyMatch(selector -> selector.contains("select")));
        Assert.assertTrue(rule.requiredCssSelectors().stream().noneMatch(selector -> selector.contains("table")));
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
