package ua.demo.agentlab.validation.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.GeneratedCodeValidator;

import java.util.List;
import java.util.Set;

public class GeneratedCodeCompileAgent implements WorkflowAgent,
        PipelineAgent<List<String>, GeneratedCodeValidationResult> {

    private final GeneratedCodeValidator validator;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public GeneratedCodeCompileAgent(GeneratedCodeValidator validator) {
        this.validator = validator;
    }

    @Override
    public String name() {
        return "generated-code-compile-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.WRITTEN_FILES);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATED_CODE_VALIDATION);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.WRITTEN_FILES;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_CODE_VALIDATION;
    }

    @Override
    public List<String> inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (store == null) {
            return state.getWrittenFiles();
        }
        return (List<String>) store.get(WorkflowArtifact.WRITTEN_FILES).orElse(state.getWrittenFiles());
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return state != null
                && state.getGeneratedCodeValidationResult() == null
                && !inputFrom(store, state).isEmpty();
    }

    @Override
    public GeneratedCodeValidationResult execute(List<String> input, WorkflowRunEnvelope run) {
        return validator.validate(input);
    }

    @Override
    public void applyOutput(GeneratedCodeValidationResult result, WorkflowState state) {
        publisher.publishGeneratedCodeValidation(result, state);
    }
}
