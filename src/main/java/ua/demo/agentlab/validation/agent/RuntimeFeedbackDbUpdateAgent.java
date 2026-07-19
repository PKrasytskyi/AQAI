package ua.demo.agentlab.validation.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.feedback.GeneratedUiRuntimeFeedbackWriter;
import ua.demo.agentlab.validation.feedback.RuntimeFeedbackDbUpdateResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionResult;

import java.util.Set;

public class RuntimeFeedbackDbUpdateAgent implements WorkflowAgent,
        PipelineAgent<RuntimeFeedbackDbUpdateAgent.Input, RuntimeFeedbackDbUpdateResult> {

    private final GeneratedUiRuntimeFeedbackWriter feedbackWriter;

    public RuntimeFeedbackDbUpdateAgent(GeneratedUiRuntimeFeedbackWriter feedbackWriter) {
        if (feedbackWriter == null) {
            throw new IllegalArgumentException("feedbackWriter cannot be null");
        }
        this.feedbackWriter = feedbackWriter;
    }

    @Override
    public String name() {
        return "runtime-feedback-db-update-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.GENERATED_UI_SMOKE_RESULT,
                WorkflowArtifact.COMPILE_RESULT,
                WorkflowArtifact.REVIEW_RESULT,
                WorkflowArtifact.GENERATED_TEST_EXECUTION_RESULT
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.RUNTIME_FEEDBACK_DB_UPDATE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.GENERATED_UI_SMOKE_RESULT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.RUNTIME_FEEDBACK_DB_UPDATE;
    }

    @Override
    public Input inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new Input(
                store == null ? null : (GeneratedUiSmokeResult) store.get(WorkflowArtifact.GENERATED_UI_SMOKE_RESULT).orElse(null),
                store == null
                        ? state.getGeneratedCodeValidationResult()
                        : (GeneratedCodeValidationResult) store.get(WorkflowArtifact.COMPILE_RESULT)
                        .orElse(state.getGeneratedCodeValidationResult()),
                store == null
                        ? state.getGeneratedCodeReviewReport()
                        : (GeneratedCodeReviewReport) store.get(WorkflowArtifact.REVIEW_RESULT)
                        .orElse(state.getGeneratedCodeReviewReport()),
                store == null ? null : store.require(WorkflowArtifact.GENERATED_TEST_EXECUTION_RESULT)
        );
    }

    @Override
    public boolean supports(Input input, WorkflowRunEnvelope run) {
        return input != null && input.smokeResult() != null;
    }

    @Override
    public RuntimeFeedbackDbUpdateResult execute(Input input, WorkflowRunEnvelope run) {
        String runId = run == null || run.runMetadata() == null ? "" : run.runMetadata().runId();
        return feedbackWriter.write(
                input.smokeResult(), input.compileResult(), input.reviewReport(), input.testExecution(), runId);
    }

    @Override
    public void applyOutput(RuntimeFeedbackDbUpdateResult output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        state.addArtifact("runtime.feedback.db.update.executed", String.valueOf(output.executed()));
        state.addArtifact("runtime.feedback.db.update.target", output.target());
        state.addArtifact("runtime.feedback.db.update.records", String.valueOf(output.recordsUpdated()));
        state.addArtifact("runtime.feedback.db.update.details", output.details());
        state.addFinding("Runtime feedback DB update: " + output.details());
    }

    public record Input(
            GeneratedUiSmokeResult smokeResult,
            GeneratedCodeValidationResult compileResult,
            GeneratedCodeReviewReport reviewReport,
            GeneratedTestExecutionResult testExecution
    ) {
    }
}
