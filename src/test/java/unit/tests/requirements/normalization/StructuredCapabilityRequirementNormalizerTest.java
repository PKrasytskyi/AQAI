package unit.tests.requirements.normalization;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;

import java.util.List;

public class StructuredCapabilityRequirementNormalizerTest {
    @Test
    public void keepsOneCanonicalRequirementPerCapabilityBlock() {
        String document = """
                ## Requirement: REQ-004 Filter Records
                ### Capability
                `FILTER`
                ### Action
                * Select Status `Active`.
                * Click Search.
                ### Expected Result
                * Results refresh and only Active rows are visible.
                ### Assertion Requirements
                * `type: RESULTS_CHANGED`
                * `type: ROW_VISIBLE`
                ### Target Context
                * `componentCapability: FILTER_PANEL, RESULTS_COLLECTION`
                """;
        var result = new RuleBasedRequirementNormalizer().normalize(new RequirementDocument("fixture.md", document));
        Assert.assertEquals(result.requirements().size(), 1);
        var requirement = result.requirements().get(0);
        Assert.assertEquals(requirement.id(), "REQ-004");
        Assert.assertEquals(requirement.expectedResult(), "Results refresh and only Active rows are visible.");
        Assert.assertTrue(requirement.tags().contains("functional-requirements"));
        Assert.assertTrue(requirement.tags().contains("capability-filter"));
        Assert.assertTrue(requirement.statement().contains("Select Status"));
        Assert.assertEquals(requirement.structuredAssertions().size(), 2);
        Assert.assertEquals(requirement.structuredAssertions().get(0).type(), "RESULTS_CHANGED");
        Assert.assertEquals(requirement.structuredSections().get("action"), List.of("Select Status `Active`.", "Click Search."));
        Assert.assertEquals(requirement.structuredSections().get("target context"),
                List.of("`componentCapability: FILTER_PANEL, RESULTS_COLLECTION`"));
    }
}
