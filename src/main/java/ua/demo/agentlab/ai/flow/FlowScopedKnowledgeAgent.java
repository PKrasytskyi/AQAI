package ua.demo.agentlab.ai.flow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.orchestration.pipeline.WorkflowStatePipelineAdapter;

import java.util.Set;

public class FlowScopedKnowledgeAgent implements WorkflowAgent,
        PipelineAgent<FlowScopedKnowledgeInput, FlowScopedKnowledgePackage>,
        WorkflowStatePipelineAdapter<FlowScopedKnowledgePackage> {

    private final FlowScopedKnowledgeService service;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public FlowScopedKnowledgeAgent(FlowScopedKnowledgeService service) {
        if (service == null) {
            throw new IllegalArgumentException("service cannot be null");
        }
        this.service = service;
    }

    @Override
    public String name() {
        return "flow-scoped-knowledge-agent";
    }

    @Override
    public int order() {
        return 28;
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.MAPPED_UI_KNOWLEDGE,
                WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE,
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.MAPPED_UI_KNOWLEDGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getMappedUiKnowledge() != null
                && state.getNormalizedRequirementBundle() != null
                && state.getFlowScopedKnowledgePackage() == null;
    }

    @Override
    public void execute(WorkflowState state) {
        applyOutput(execute(inputFrom(null, state), WorkflowRunEnvelope.from(state)), state);
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
    public boolean supports(FlowScopedKnowledgeInput input, WorkflowRunEnvelope run) {
        return input != null
                && input.mappedUiKnowledge() != null
                && input.normalizedRequirementBundle() != null;
    }

    @Override
    public FlowScopedKnowledgePackage execute(FlowScopedKnowledgeInput input, WorkflowRunEnvelope run) {
        return service.scope(input);
    }

    @Override
    public void applyOutput(FlowScopedKnowledgePackage output, WorkflowState state) {
        outputPublisher.publishFlowScopedKnowledgePackage(output, state);
    }
}
