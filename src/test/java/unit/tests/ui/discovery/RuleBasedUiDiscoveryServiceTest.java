package unit.tests.ui.discovery;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.ui.discovery.RuleBasedUiDiscoveryService;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiFlow;
import ua.demo.agentlab.ui.discovery.model.UiDiscoveryResult;

import java.util.List;

public class RuleBasedUiDiscoveryServiceTest {

    @Test
    public void accountProfilePhraseDoesNotBecomeProfileRoute() {
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
                        new SourceReference("requirements/demo.md", 10, 10, "Account/profile area")
                )),
                List.of(),
                List.of()
        );

        UiDiscoveryResult result = new RuleBasedUiDiscoveryService().discover(profile, requirements);

        Assert.assertTrue(result.snapshot().pages().stream()
                .noneMatch(page -> "/profile".equals(page.route())));
        DiscoveredUiFlow flow = result.snapshot().flows().get(0);
        Assert.assertEquals(flow.targetPageName(), "DetailPage");
        Assert.assertEquals(flow.targetRoute(), "");
        Assert.assertTrue(flow.stepDescriptions().stream().anyMatch(step -> step.contains("needs evidence")));
    }
}
