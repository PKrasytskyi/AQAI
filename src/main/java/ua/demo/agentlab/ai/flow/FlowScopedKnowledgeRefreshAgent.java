package ua.demo.agentlab.ai.flow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.util.Set;

public class FlowScopedKnowledgeRefreshAgent implements WorkflowAgent,
        PipelineAgent<FlowScopedKnowledgeInput, FlowScopedKnowledgePackage> {

    private final FlowScopedKnowledgeService service;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public FlowScopedKnowledgeRefreshAgent(FlowScopedKnowledgeService service) {
        if (service == null) {
            throw new IllegalArgumentException("service cannot be null");
        }
        this.service = service;
    }

    @Override
    public String name() {
        return "flow-scoped-knowledge-refresh-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE,
                WorkflowArtifact.UI_KNOWLEDGE_PERSISTED
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE;
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return state != null
                && state.getFlowScopedKnowledgePackage() != null
                && "true".equals(state.getArtifacts().get("ui.knowledge.persistence.completed"))
                && !state.getArtifacts().containsKey("flow.scoped.knowledge.refreshed");
    }

    @Override
    public FlowScopedKnowledgeInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new FlowScopedKnowledgeInput(
                state.getObjective(),
                state.getProjectProfile(),
                state.getTestPlan(),
                state.getNormalizedRequirementBundle(),
                state.getCanonicalTestCaseBundle(),
                state.getMappedUiKnowledge(),
                state.getKnowledgeRunMetadata()
        );
    }

    @Override
    public FlowScopedKnowledgePackage execute(FlowScopedKnowledgeInput input, WorkflowRunEnvelope run) {
        return service.scope(input);
    }

    @Override
    public void applyOutput(FlowScopedKnowledgePackage output, WorkflowState state) {
        outputPublisher.publishRefreshedFlowScopedKnowledgePackage(output, state);
    }
}
