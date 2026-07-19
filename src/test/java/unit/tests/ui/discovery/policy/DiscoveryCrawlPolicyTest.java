package unit.tests.ui.discovery.policy;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.ui.discovery.policy.DiscoveryCrawlPolicy;

import java.util.List;
import java.util.Map;

public class DiscoveryCrawlPolicyTest {

    @Test
    public void absoluteStartUrlsPreserveApplicationBasePath() {
        ProjectProfile profile = new ProjectProfile(
                "orangehrm",
                "OrangeHRM",
                "https://opensource-demo.orangehrmlive.com/web/index.php",
                "/auth/login",
                "/auth/login",
                "/register",
                "/dashboard/index",
                "/recover",
                "/details",
                "/auth/login",
                "/dashboard/index",
                "/dashboard/index",
                "/dashboard/index",
                "/cart",
                new OutputProfile("pages", "tests")
        );
        DiscoveryCrawlPolicy policy = new DiscoveryCrawlPolicy(
                List.of("/auth/login", "/dashboard/index"),
                1,
                2,
                true,
                true,
                false,
                true,
                false,
                true,
                List.of(),
                List.of()
        );

        Assert.assertEquals(
                policy.absoluteStartUrls(profile),
                List.of(
                        "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login",
                        "https://opensource-demo.orangehrmlive.com/web/index.php/dashboard/index"
                )
        );
    }

    @Test
    public void accountProfilePhraseDoesNotBecomeCrawlStartUrl() {
        ProjectProfile profile = new ProjectProfile(
                "orangehrm",
                "OrangeHRM",
                "https://opensource-demo.orangehrmlive.com/web/index.php",
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
                new OutputProfile("pages", "tests")
        );
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/demo.md",
                List.of(new NormalizedRequirement(
                        "REQ-ACCOUNT",
                        "Account / Profile",
                        "Account/profile area shall show user identity or account-related navigation.",
                        "",
                        true,
                        false,
                        List.of("account", "ui"),
                        new SourceReference("requirements/demo.md", 1, 1, "Account/profile area")
                )),
                List.of(),
                List.of()
        );
        DiscoveryCrawlPolicy policy = DiscoveryCrawlPolicy.defaultPolicy(profile);

        List<String> urls = policy.absoluteStartUrls(profile, requirements);

        Assert.assertFalse(urls.contains("https://opensource-demo.orangehrmlive.com/web/index.php/profile"));
        Assert.assertTrue(urls.contains("https://opensource-demo.orangehrmlive.com/web/index.php/dashboard/index"));
    }

    @Test
    public void explicitStructuredRoutesArePrioritizedAndExpandTheSeedBudget() {
        ProjectProfile profile = new ProjectProfile(
                "demo", "Demo", "https://example.test", "/", "/login", "", "/secure",
                "", "", "", "", "", "", "", new OutputProfile("pages", "tests")
        );
        NormalizedRequirement first = new NormalizedRequirement(
                "REQ-1", "Upload", "Upload a file.", "Upload succeeds.", true, false,
                List.of("structured-requirement", "capability-file-upload"),
                new SourceReference("requirements/demo.md", 1, 1, ""), List.of(),
                Map.of("target context", List.of("`targetRoute: /upload`"))
        );
        NormalizedRequirement second = new NormalizedRequirement(
                "REQ-2", "New window", "Open a new window.", "Window opens.", true, false,
                List.of("structured-requirement", "capability-window-management"),
                new SourceReference("requirements/demo.md", 10, 10, ""), List.of(),
                Map.of("target context", List.of("`sourceRoute: /windows`", "`targetRoute: /windows/new`"))
        );
        DiscoveryCrawlPolicy policy = new DiscoveryCrawlPolicy(
                List.of("/"), 1, 2, true, true, false, true, false, true, List.of(), List.of()
        );
        NormalizedRequirementBundle bundle = new NormalizedRequirementBundle(
                "requirements/demo.md", List.of(first, second), List.of(), List.of()
        );

        List<String> urls = policy.absoluteStartUrls(profile, bundle);
        Assert.assertTrue(urls.contains("https://example.test/upload"));
        Assert.assertTrue(urls.contains("https://example.test/windows"));
        Assert.assertTrue(urls.contains("https://example.test/windows/new"));
        Assert.assertEquals(policy.effectiveMaxPages(profile, bundle), urls.size());
    }

    @Test
    public void safePageRouteContainingRemoveIsNotBlockedAsDestructiveAction() {
        ProjectProfile profile = new ProjectProfile(
                "the-internet", "The Internet", "https://the-internet.herokuapp.com", "/", "/login", "", "/secure",
                "", "", "", "", "", "", "", new OutputProfile("pages", "tests")
        );
        DiscoveryCrawlPolicy policy = DiscoveryCrawlPolicy.defaultPolicy(profile);

        Assert.assertTrue(policy.allowsNavigation(profile.baseUrl(),
                "https://the-internet.herokuapp.com/add_remove_elements/"));
        Assert.assertTrue(policy.isBlockedAction("Remove"),
                "The destructive click remains blocked even though the page route is discoverable");
    }
}
