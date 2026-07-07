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
}
