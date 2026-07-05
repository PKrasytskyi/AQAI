package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.ui.generation.AiUiTestGenerationRequest;
import ua.demo.agentlab.ai.ui.generation.AiUiTestGenerationResult;
import ua.demo.agentlab.ai.ui.generation.AiUiTestSpecGenerator;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.util.List;
import java.util.Set;

public class AiUiTestSpecAgent implements WorkflowAgent,
        PipelineAgent<AiUiTestGenerationRequest, AiUiTestGenerationResult> {

    private final AiUiTestSpecGenerator generator;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public AiUiTestSpecAgent(AiUiTestSpecGenerator generator) {
        if (generator == null) {
            throw new IllegalArgumentException("generator cannot be null");
        }
        this.generator = generator;
    }

    @Override
    public String name() {
        return "ai-ui-test-spec-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.UI_TEST_PLAN,
                WorkflowArtifact.AI_CONTEXT_PACKAGE,
                WorkflowArtifact.AI_PAGE_OBJECT_SPECS
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.AI_UI_TEST_GENERATION_RESULT,
                WorkflowArtifact.AI_UI_TEST_SPECS
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.AI_CONTEXT_PACKAGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.AI_UI_TEST_GENERATION_RESULT;
    }

    @Override
    public AiUiTestGenerationRequest inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new AiUiTestGenerationRequest(
                WorkflowRunEnvelope.from(state),
                store == null
                        ? state.getAiContextPackage()
                        : (ua.demo.agentlab.ai.context.AiContextPackage)
                        store.get(WorkflowArtifact.AI_CONTEXT_PACKAGE).orElse(state.getAiContextPackage()),
                store == null
                        ? state.getUiTestPlan()
                        : (ua.demo.agentlab.ui.UiTestPlan)
                        store.get(WorkflowArtifact.UI_TEST_PLAN).orElse(state.getUiTestPlan()),
                store == null
                        ? state.getAiPageObjectSpecs()
                        : (List<AiPageObjectSpec>)
                        store.get(WorkflowArtifact.AI_PAGE_OBJECT_SPECS).orElse(state.getAiPageObjectSpecs()),
                store == null
                        ? state.getAiUiTestSpecs()
                        : (List<AiUiTestSpec>)
                        store.get(WorkflowArtifact.AI_UI_TEST_SPECS).orElse(state.getAiUiTestSpecs())
        );
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return state != null
                && state.getAiUiTestSpecs().isEmpty()
                && inputFrom(store, state).uiTestPlan() != null
                && inputFrom(store, state).contextPackage() != null;
    }

    @Override
    public AiUiTestGenerationResult execute(AiUiTestGenerationRequest input, WorkflowRunEnvelope run) {
        return generator.generate(input);
    }

    @Override
    public void applyOutput(AiUiTestGenerationResult output, WorkflowState state) {
        outputPublisher.publishAiUiTestGenerationResult(output, state);
    }
}
