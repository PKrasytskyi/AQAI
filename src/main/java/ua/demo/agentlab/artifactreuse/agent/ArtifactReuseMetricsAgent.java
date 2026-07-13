package ua.demo.agentlab.artifactreuse.agent;

import ua.demo.agentlab.artifactreuse.lifecycle.ArtifactLifecycleResult;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseMetricsWriteResult;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseRunMetrics;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseRunMetricsCollector;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseRunMetricsWriter;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.util.Map;
import java.util.Set;

public class ArtifactReuseMetricsAgent implements WorkflowAgent,
        PipelineAgent<Map<String, String>, ArtifactReuseRunMetrics> {

    private final ArtifactReuseRunMetricsCollector collector;
    private final ArtifactReuseRunMetricsWriter writer;

    public ArtifactReuseMetricsAgent() {
        this(new ArtifactReuseRunMetricsCollector(), new ArtifactReuseRunMetricsWriter());
    }

    ArtifactReuseMetricsAgent(ArtifactReuseRunMetricsCollector collector, ArtifactReuseRunMetricsWriter writer) {
        if (collector == null || writer == null) {
            throw new IllegalArgumentException("artifact reuse metrics dependencies cannot be null");
        }
        this.collector = collector;
        this.writer = writer;
    }

    @Override
    public String name() {
        return "artifact-reuse-metrics-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.ARTIFACT_LIFECYCLE_RESULT);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.ARTIFACT_REUSE_METRICS);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.ARTIFACT_LIFECYCLE_RESULT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.ARTIFACT_REUSE_METRICS;
    }

    @Override
    public Map<String, String> inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return state == null ? Map.of() : state.getArtifacts();
    }

    @Override
    public boolean supports(Map<String, String> input, WorkflowRunEnvelope run) {
        return input != null && input.containsKey("artifact.reuse.enabled");
    }

    @Override
    public ArtifactReuseRunMetrics execute(Map<String, String> input, WorkflowRunEnvelope run) {
        return collector.collect(input);
    }

    @Override
    public void applyOutput(ArtifactReuseRunMetrics output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        ArtifactReuseMetricsWriteResult result = writer.write(output);
        state.addArtifact("artifact.reuse.metrics.file", result.metricsFile() == null ? "" : result.metricsFile().toString());
        state.addArtifact("artifact.reuse.metrics.write.success", String.valueOf(result.success()));
        state.addArtifact("artifact.reuse.metrics.llm.executed", String.valueOf(output.llmCallsExecuted()));
        state.addArtifact("artifact.reuse.metrics.llm.skipped", String.valueOf(output.llmCallsSkipped()));
        state.addArtifact("artifact.reuse.metrics.stable.count", String.valueOf(output.stableArtifacts()));
        state.addArtifact("artifact.reuse.metrics.needs-review.count", String.valueOf(output.needsReviewArtifacts()));
        state.addFinding("Artifact reuse metrics: " + output.artifactCacheHits() + " hit(s), "
                + output.llmCallsSkipped() + " POM LLM call(s) skipped");
    }
}
