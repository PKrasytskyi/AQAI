package unit.tests.testcase.planning;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseInput;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.testcase.planning.ScenarioPipelineRequirementToTestCaseGenerator;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public class ScenarioPipelineRequirementToTestCaseGeneratorTest {

    @Test
    public void doesNotInventRecordDetailsRouteWhenProfileDiscoveryAndCacheDoNotConfirmIt() {
        ProjectProfile profile = new ProjectProfile(
                "demo",
                "Demo App",
                "https://example.test",
                "/",
                "/auth/login",
                "",
                "/dashboard/index",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new OutputProfile("pages", "tests")
        );
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/demo.md",
                List.of(new NormalizedRequirement(
                        "REQ-001",
                        "User can inspect an account profile summary",
                        "The user can open the account profile page.",
                        "Account profile summary is visible.",
                        true,
                        false,
                        List.of("functional-requirements"),
                        new SourceReference("requirements/demo.md", 1, 1, "")
                )),
                List.of(),
                List.of()
        );

        CanonicalTestCaseBundle bundle = new ScenarioPipelineRequirementToTestCaseGenerator().generate(
                new RequirementToTestCaseInput(profile, requirements, MappedUiKnowledge.empty(), null)
        );

        Assert.assertEquals(bundle.testCases().size(), 1);
        var testCase = bundle.testCases().get(0);
        Assert.assertEquals(testCase.pageName(), "GenericPage");
        Assert.assertEquals(testCase.route(), "");
        Assert.assertFalse(testCase.targetPages().contains("DetailPage"));
        Assert.assertFalse(testCase.targetPages().contains("RecordDetailsPage"));
    }
}
