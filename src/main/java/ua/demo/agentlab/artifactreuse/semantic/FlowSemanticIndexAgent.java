package ua.demo.agentlab.artifactreuse.semantic;

import ua.demo.agentlab.artifactreuse.flow.FlowContractBundle;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.util.Set;

public class FlowSemanticIndexAgent implements WorkflowAgent, PipelineAgent<FlowContractBundle, FlowSemanticIndexResult> {

    private final FlowSemanticIndexer indexer;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public FlowSemanticIndexAgent(FlowSemanticIndexer indexer) {
        if (indexer == null) throw new IllegalArgumentException("flow semantic indexer cannot be null");
        this.indexer = indexer;
    }

    @Override public String name() { return "flow-semantic-index-agent"; }
    @Override public Set<WorkflowArtifact> requires() { return Set.of(WorkflowArtifact.FLOW_CONTRACT_BUNDLE); }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.FLOW_SEMANTIC_INDEX_RESULT); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.FLOW_CONTRACT_BUNDLE; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.FLOW_SEMANTIC_INDEX_RESULT; }

    @Override
    public FlowContractBundle inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return (FlowContractBundle) store.get(WorkflowArtifact.FLOW_CONTRACT_BUNDLE)
                .orElse(new FlowContractBundle("flow-contract-bundle.v1", null, java.util.List.of()));
    }

    @Override public boolean supports(FlowContractBundle input, WorkflowRunEnvelope run) { return input != null; }
    @Override public FlowSemanticIndexResult execute(FlowContractBundle input, WorkflowRunEnvelope run) { return indexer.index(input); }

    @Override
    public void applyOutput(FlowSemanticIndexResult output, WorkflowState state) {
        if (output == null || state == null) return;
        state.addArtifact("artifact.reuse.semantic.index.attempted", String.valueOf(output.attempted()));
        state.addArtifact("artifact.reuse.semantic.index.success", String.valueOf(output.success()));
        state.addArtifact("artifact.reuse.semantic.index.count", String.valueOf(output.indexedCount()));
        state.addArtifact("artifact.reuse.semantic.index.message", output.message());
        artifactPublisher.writeJson(state, "artifact-reuse", "flow-semantic-index.json", output);
    }
}
