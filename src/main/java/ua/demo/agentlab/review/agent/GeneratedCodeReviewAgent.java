package ua.demo.agentlab.review.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.review.GeneratedCodeReviewer;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.persistence.GeneratedUiSources;

import java.util.List;
import java.util.Set;

public class GeneratedCodeReviewAgent implements WorkflowAgent,
        PipelineAgent<GeneratedUiSources, GeneratedCodeReviewReport> {

    private final GeneratedCodeReviewer reviewer;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public GeneratedCodeReviewAgent(GeneratedCodeReviewer reviewer){
        this.reviewer = reviewer;
    }

    @Override
    public String name() {
        return "generated-code-review-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.PAGE_OBJECT_FILES);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATED_CODE_REVIEW);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.PAGE_OBJECT_FILES;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_CODE_REVIEW;
    }

    @Override
    public GeneratedUiSources inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new GeneratedUiSources(
                store == null ? state.getPageObjectFiles() : (List<ua.demo.agentlab.ui.writer.GeneratedSourceFile>)
                        store.get(WorkflowArtifact.PAGE_OBJECT_FILES).orElse(state.getPageObjectFiles()),
                store == null ? state.getUiTestFiles() : (List<ua.demo.agentlab.ui.writer.GeneratedSourceFile>)
                        store.get(WorkflowArtifact.UI_TEST_FILES).orElse(state.getUiTestFiles())
        );
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return state != null
                && state.getGeneratedCodeReviewReport() == null
                && !inputFrom(store, state).isEmpty();
    }

    @Override
    public GeneratedCodeReviewReport execute(GeneratedUiSources input, WorkflowRunEnvelope run) {
        return reviewer.review(input);
    }

    @Override
    public void applyOutput(GeneratedCodeReviewReport report, WorkflowState state) {
        publisher.publishGeneratedCodeReview(report, state);
    }
}
