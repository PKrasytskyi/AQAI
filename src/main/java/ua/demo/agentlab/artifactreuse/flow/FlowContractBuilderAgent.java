package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

import java.util.Set;

public class FlowContractBuilderAgent implements WorkflowAgent,
        PipelineAgent<FlowContractBuilderInput, FlowContractBundle> {

    private final FlowContractBuilder builder;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public FlowContractBuilderAgent(FlowContractBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("flow contract builder cannot be null");
        }
        this.builder = builder;
    }

    @Override
    public String name() {
        return "flow-contract-builder-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE, WorkflowArtifact.CANONICAL_PAGE_FLOW_MODEL,
                WorkflowArtifact.MAPPED_UI_KNOWLEDGE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.FLOW_CONTRACT_BUNDLE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.FLOW_CONTRACT_BUNDLE;
    }

    @Override
    public FlowContractBuilderInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        KnowledgeRunMetadata runMetadata = state.getKnowledgeRunMetadata();
        if (runMetadata == null) {
            runMetadata = KnowledgeRunMetadata.from(state, name());
        }
        return new FlowContractBuilderInput(
                (CanonicalTestCaseBundle) store.get(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE)
                        .orElse(state.getCanonicalTestCaseBundle()),
                (CanonicalPageFlowModel) store.get(WorkflowArtifact.CANONICAL_PAGE_FLOW_MODEL)
                        .orElse(state.getCanonicalPageFlowModel()),
                (MappedUiKnowledge) store.get(WorkflowArtifact.MAPPED_UI_KNOWLEDGE)
                        .orElse(state.getMappedUiKnowledge()),
                runMetadata
        );
    }

    @Override
    public boolean supports(FlowContractBuilderInput input, WorkflowRunEnvelope run) {
        return input != null && input.canonicalTestCases() != null && !input.canonicalTestCases().testCases().isEmpty();
    }

    @Override
    public FlowContractBundle execute(FlowContractBuilderInput input, WorkflowRunEnvelope run) {
        return builder.build(input);
    }

    @Override
    public void applyOutput(FlowContractBundle output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        if (output.runMetadata() != null && state.getKnowledgeRunMetadata() == null) {
            state.setKnowledgeRunMetadata(output.runMetadata());
            state.addArtifact("ui.knowledge.run.id", output.runMetadata().runId());
            state.addArtifact("ui.knowledge.app.id", output.runMetadata().appId());
            state.addArtifact("ui.knowledge.schema.version", output.runMetadata().schemaVersion());
        }
        state.addArtifact("flow.contract.count", String.valueOf(output.contracts().size()));
        state.addArtifact("flow.contract.confirmed.count", String.valueOf(output.confirmedCount()));
        state.addArtifact("flow.contract.needs-review.count", String.valueOf(output.needsReviewCount()));
        artifactPublisher.writeJson(state, "flow-contracts", "flow-contracts.json", output);
        state.addFinding("Flow contracts built: " + output.confirmedCount() + " confirmed, "
                + output.needsReviewCount() + " needs review");
    }
}
