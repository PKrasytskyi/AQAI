package unit.tests.requirements.behavior;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContractBuilder;
import ua.demo.agentlab.requirements.normalization.model.*;
import java.util.List;

public class StructuredBehaviorContractBuilderTest {
    @Test
    public void marksFilterExecutableOnlyWithDataAndObservablePostcondition() {
        NormalizedRequirement requirement = new NormalizedRequirement("REQ-004","Filter records",
                "Capability: FILTER. Action: Select Status. Click Search. Target Context: table. Assertion Requirements: type: RESULTS_CHANGED. Data Requirements: dataset: vacancy-filter.",
                "Results refresh.",true,false,List.of("structured-requirement","capability-filter"),new SourceReference("x",1,1,""),
                List.of(new StructuredAssertionRequirement("RESULTS_CHANGED","results","changed",null)));
        var contract = new StructuredBehaviorContractBuilder().build(List.of(requirement)).get(0);
        Assert.assertTrue(contract.executable());
        Assert.assertEquals(contract.capability(),"filter");
    }

    @Test
    public void keepsFilterReadinessExecutableWithoutScenarioDataOrResultMutation() {
        NormalizedRequirement requirement = new NormalizedRequirement("REQ-003", "Filter controls ready",
                "legacy", "", true, false, List.of("structured-requirement", "capability-filter"),
                new SourceReference("x", 1, 1, ""),
                List.of(new StructuredAssertionRequirement("ELEMENT_VISIBLE", "searchButton", "visible", null)),
                java.util.Map.of("action", List.of("Inspect the vacancy filter controls and the Search action."),
                        "target context", List.of("pageCapability: RECORD_LIST"),
                        "data requirements", List.of("No scenario data is required.")));

        var contract = new StructuredBehaviorContractBuilder().build(List.of(requirement)).get(0);

        Assert.assertTrue(contract.executable());
        Assert.assertTrue(contract.dataRequirements().isEmpty());
    }

    @Test
    public void removesMarkdownBackticksFromTargetContextAndDataValues() {
        NormalizedRequirement requirement = new NormalizedRequirement("REQ-005", "Filter records", "legacy", "", true, false,
                List.of("structured-requirement", "capability-filter"), new SourceReference("x", 1, 1, ""),
                List.of(new StructuredAssertionRequirement("RESULTS_CHANGED", "results", "changed", null)),
                java.util.Map.of("action", List.of("Select status."),
                        "target context", List.of("`pageCapability: RECORD_LIST`", "`targetRoute: discovery-confirmed`"),
                        "data requirements", List.of("`dataset: vacancy-filter`", "`JOB_TITLE: ${JOB_TITLE}`")));

        var contract = new StructuredBehaviorContractBuilder().build(List.of(requirement)).get(0);

        Assert.assertEquals(contract.targetContext(), "pageCapability: RECORD_LIST targetRoute: discovery-confirmed");
        Assert.assertEquals(contract.dataRequirements().get("dataset"), "vacancy-filter");
        Assert.assertEquals(contract.dataRequirements().get("JOB_TITLE"), "${JOB_TITLE}");
    }
}
