package ua.demo.agentlab.review.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.review.GeneratedCodeReviewer;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;

public class GeneratedCodeReviewAgent implements WorkflowAgent {

    private final GeneratedCodeReviewer reviewer;

    public GeneratedCodeReviewAgent(GeneratedCodeReviewer reviewer){
        this.reviewer = reviewer;
    }

    @Override
    public String name() {
        return "generated-code-review-agent";
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return !state.getPageObjectFiles().isEmpty() || !state.getUiTestFiles().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        GeneratedCodeReviewReport report = reviewer.review(state);

        state.setGeneratedCodeReviewReport(report);
        state.addArtifact("generated.code.review.findings", String.valueOf(report.totalFindings()));
        state.addArtifact("generated.code.review.summary", report.summary());
        state.addFinding(report.summary());
    }
}
