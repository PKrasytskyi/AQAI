package ua.demo.agentlab.ui.discovery.policy;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;

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
}
