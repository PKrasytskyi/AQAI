package ua.demo.agentlab.artifactreuse.agent;

import ua.demo.agentlab.artifactreuse.metrics.RunHistoryStatisticsReport;
import ua.demo.agentlab.artifactreuse.metrics.RunHistoryStatisticsReporter;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.validation.feedback.RuntimeFeedbackDbUpdateResult;

import java.util.Set;

/** Writes the final, cross-run table only after validation and DB feedback stages have completed. */
public class RunHistoryStatisticsAgent implements WorkflowAgent,
        PipelineAgent<RuntimeFeedbackDbUpdateResult, RunHistoryStatisticsReport> {

    private final RunHistoryStatisticsReporter reporter;

    public RunHistoryStatisticsAgent() {
        this(new RunHistoryStatisticsReporter());
    }

    RunHistoryStatisticsAgent(RunHistoryStatisticsReporter reporter) {
        if (reporter == null) {
            throw new IllegalArgumentException("reporter cannot be null");
        }
        this.reporter = reporter;
    }

    @Override public String name() { return "run-history-statistics-agent"; }
    @Override public Set<WorkflowArtifact> requires() { return Set.of(WorkflowArtifact.RUNTIME_FEEDBACK_DB_UPDATE); }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.RUN_HISTORY_STATISTICS); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.RUNTIME_FEEDBACK_DB_UPDATE; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.RUN_HISTORY_STATISTICS; }

    @Override
    public RuntimeFeedbackDbUpdateResult inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return store == null ? null : (RuntimeFeedbackDbUpdateResult) store
                .get(WorkflowArtifact.RUNTIME_FEEDBACK_DB_UPDATE).orElse(null);
    }

    @Override
    public boolean supports(RuntimeFeedbackDbUpdateResult input, WorkflowRunEnvelope run) {
        return input != null;
    }

    @Override
    public RunHistoryStatisticsReport execute(RuntimeFeedbackDbUpdateResult input, WorkflowRunEnvelope run) {
        return reporter.writeLatestRuns();
    }

    @Override
    public void applyOutput(RunHistoryStatisticsReport output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        state.addArtifact("run.history.statistics.file", output.markdownFile().toString());
        state.addArtifact("run.history.statistics.count", String.valueOf(output.runs().size()));
        state.addFinding("Run history statistics updated for " + output.runs().size() + " completed run(s)");
    }
}
