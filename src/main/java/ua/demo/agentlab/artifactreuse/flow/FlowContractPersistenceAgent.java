package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.util.Set;

public class FlowContractPersistenceAgent implements WorkflowAgent,
        PipelineAgent<FlowContractBundle, FlowContractPersistenceResult> {

    private final ArtifactReuseRuntimeConfig config;
    private final FlowContractRegistry registry;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public FlowContractPersistenceAgent(ArtifactReuseRuntimeConfig config, FlowContractRegistry registry) {
        if (config == null || registry == null) {
            throw new IllegalArgumentException("flow contract persistence dependencies cannot be null");
        }
        this.config = config;
        this.registry = registry;
    }

    @Override
    public String name() {
        return "flow-contract-persistence-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.FLOW_CONTRACT_BUNDLE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.FLOW_CONTRACT_PERSISTENCE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.FLOW_CONTRACT_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.FLOW_CONTRACT_PERSISTENCE;
    }

    @Override
    public FlowContractBundle inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return (FlowContractBundle) store.get(WorkflowArtifact.FLOW_CONTRACT_BUNDLE)
                .orElse(new FlowContractBundle("flow-contract-bundle.v1", null, java.util.List.of()));
    }

    @Override
    public boolean supports(FlowContractBundle input, WorkflowRunEnvelope run) {
        return input != null;
    }

    @Override
    public FlowContractPersistenceResult execute(FlowContractBundle input, WorkflowRunEnvelope run) {
        if (!config.flowContractEnabled()) {
            return FlowContractPersistenceResult.skipped("neo4j", "Flow contract persistence is disabled");
        }
        return registry.persist(input);
    }

    @Override
    public void applyOutput(FlowContractPersistenceResult output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        state.addArtifact("flow.contract.persistence.attempted", String.valueOf(output.attempted()));
        state.addArtifact("flow.contract.persistence.success", String.valueOf(output.success()));
        state.addArtifact("flow.contract.persistence.backend", output.backend());
        state.addArtifact("flow.contract.persistence.count", String.valueOf(output.contractsPersisted()));
        state.addArtifact("flow.contract.persistence.message", output.message());
        artifactPublisher.writeJson(state, "flow-contracts", "flow-contract-persistence.json", output);
        state.addFinding("Flow contract persistence: " + output.message());
    }
}
