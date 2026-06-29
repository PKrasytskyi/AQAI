package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.context.AiContextAssemblyInput;
import ua.demo.agentlab.ai.context.AiContextAssembler;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.orchestration.pipeline.WorkflowStatePipelineAdapter;

import java.util.Set;

public class AiContextAssemblyAgent implements WorkflowAgent,
        PipelineAgent<AiContextAssemblyInput, AiContextPackage>,
        WorkflowStatePipelineAdapter<AiContextPackage> {

    private final AiContextAssembler contextAssembler;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public AiContextAssemblyAgent(AiContextAssembler contextAssembler) {
        if (contextAssembler == null) {
            throw new IllegalArgumentException("contextAssembler cannot be null");
        }
        this.contextAssembler = contextAssembler;
    }

    @Override
    public String name() {
        return "ai-context-assembly-agent";
    }

    @Override
    public int order() {
        return 34;
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.PAGE_MODEL_ENRICHMENT_RECORDS,
                WorkflowArtifact.REFRESHED_FLOW_SCOPED_KNOWLEDGE_PACKAGE,
                WorkflowArtifact.ASSERTION_CONTRACTS
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.AI_CONTEXT_PACKAGE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.ASSERTION_CONTRACTS;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.AI_CONTEXT_PACKAGE;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getMappedUiKnowledge() != null && state.getAiContextPackage() == null;
    }

    @Override
    public boolean supports(AiContextAssemblyInput input, WorkflowRunEnvelope run) {
        return input != null
                && input.mappedUiKnowledge() != null
                && input.canonicalTestCaseBundle() != null
                && input.assertionContracts() != null;
    }

    @Override
    public void execute(WorkflowState state) {
        AiContextPackage contextPackage = execute(AiContextAssemblyInput.from(state), WorkflowRunEnvelope.from(state));
        applyOutput(contextPackage, state);
    }

    @Override
    public AiContextAssemblyInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return AiContextAssemblyInput.from(state);
    }

    @Override
    public void applyOutput(AiContextPackage contextPackage, WorkflowState state) {
        outputPublisher.publishAiContextPackage(contextPackage, state);
    }

    @Override
    public AiContextPackage execute(AiContextAssemblyInput input, WorkflowRunEnvelope run) {
        return contextAssembler.assemble(input);
    }
}
