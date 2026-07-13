package ua.demo.agentlab.artifactreuse.planner;

import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBundle;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.util.Set;

public class ReusePlannerAgent implements WorkflowAgent, PipelineAgent<ReusePlannerInput, ReusePlannerResult> {

    private final ReusePlanner planner;
    private final ArtifactReuseRuntimeConfig config;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public ReusePlannerAgent(ReusePlanner planner, ArtifactReuseRuntimeConfig config) {
        if (planner == null || config == null) throw new IllegalArgumentException("reuse planner dependencies cannot be null");
        this.planner = planner;
        this.config = config;
    }

    @Override public String name() { return "reuse-planner-agent"; }
    @Override public Set<WorkflowArtifact> requires() { return Set.of(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE, WorkflowArtifact.FLOW_CONTRACT_BUNDLE, WorkflowArtifact.FLOW_SEMANTIC_CANDIDATES); }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.REUSE_PLANNER_RESULT); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.REUSE_PLANNER_RESULT; }

    @Override
    public ReusePlannerInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new ReusePlannerInput(
                (CanonicalTestCaseBundle) store.get(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE).orElse(state.getCanonicalTestCaseBundle()),
                (FlowContractBundle) store.get(WorkflowArtifact.FLOW_CONTRACT_BUNDLE)
                        .orElse(new FlowContractBundle("flow-contract-bundle.v1", state.getKnowledgeRunMetadata(), java.util.List.of())),
                (FlowSemanticCandidateBundle) store.get(WorkflowArtifact.FLOW_SEMANTIC_CANDIDATES)
                        .orElse(FlowSemanticCandidateBundle.unavailable("semantic candidates are unavailable")),
                config.enabled() && config.flowContractEnabled(), config.explainDecisions());
    }

    @Override public boolean supports(ReusePlannerInput input, WorkflowRunEnvelope run) { return input != null && input.canonicalTestCases() != null; }
    @Override public ReusePlannerResult execute(ReusePlannerInput input, WorkflowRunEnvelope run) { return planner.plan(input); }

    @Override
    public void applyOutput(ReusePlannerResult output, WorkflowState state) {
        if (output == null || state == null) return;
        state.addArtifact("artifact.reuse.flow.stable.count", String.valueOf(output.stableReuseCount()));
        state.addArtifact("artifact.reuse.flow.discover.count", String.valueOf(output.discoveryCount()));
        state.addArtifact("artifact.reuse.flow.needs-review.count", String.valueOf(output.needsReviewCount()));
        state.addArtifact("artifact.reuse.flow.vector-unavailable-reason", output.vectorUnavailableReason());
        artifactPublisher.writeJson(state, "artifact-reuse", "reuse-plan.json", output);
    }
}
