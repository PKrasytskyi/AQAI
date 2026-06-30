package ua.demo.agentlab.orchestration.pipeline;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.expectationenrichment.agent.TestCaseExpectationEnrichmentOutput;
import ua.demo.agentlab.ai.expectationenrichment.model.ResolvedExpectedResult;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class StageOutputPublisherTest {

    @Test
    public void writesNeedsReviewExpectedResultsToSeparateAiRunFolder() {
        StageOutputPublisher publisher = new StageOutputPublisher();
        WorkflowState state = new WorkflowState(
                "Generate prompts",
                new RequirementInput(SourceType.FILE, "requirements.md")
        );
        String testCaseId = "NEEDS_REVIEW_STAGE_OUTPUT_TEST";
        TestCaseExpectationEnrichmentOutput output = new TestCaseExpectationEnrichmentOutput(
                new CanonicalTestCaseBundle("test", "LoginPage", List.of("LoginPage"), List.of()),
                List.of(),
                List.of(new ResolvedExpectedResult(
                        testCaseId,
                        "",
                        "REQ-X",
                        "needs-review",
                        0.50d,
                        "needs-review",
                        "No deterministic assertion requirement matched."
                )),
                List.of()
        );

        publisher.publishExpectationEnrichment(output, state);

        Path aggregate = Path.of("target", "ai-run", "need-review", "expected-results-needs-review.json");
        Path perCase = Path.of("target", "ai-run", "need-review", testCaseId + "-expected-result.json");
        Assert.assertTrue(Files.exists(aggregate), "Expected aggregate needs-review artifact");
        Assert.assertTrue(Files.exists(perCase), "Expected per-testcase needs-review artifact");
        Assert.assertEquals(state.getArtifacts().get("test.case.expectation.needs.review.count"), "1");
    }
}
