package ua.demo.agentlab.validation.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
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
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public GeneratedCodeCompileAgent(GeneratedCodeValidator validator) {
        this.validator = validator;
    }

    @Override
    public String name() {
        return "generated-code-compile-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.PERSISTED_GENERATED_SOURCES,
                WorkflowArtifact.GENERATED_UI_CONTRACT_VALIDATION
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.COMPILE_RESULT, WorkflowArtifact.GENERATED_CODE_VALIDATION);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.PERSISTED_GENERATED_SOURCES;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.COMPILE_RESULT;
    }

    @Override
    public List<String> inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (store == null) {
            return state.getWrittenFiles();
        }
        List<String> persistedSources = store.getList(
                WorkflowArtifact.PERSISTED_GENERATED_SOURCES,
                String.class
        );
        if (!persistedSources.isEmpty()) {
            return persistedSources;
        }
        List<String> writtenFiles = store.getList(WorkflowArtifact.WRITTEN_FILES, String.class);
        return writtenFiles.isEmpty() ? state.getWrittenFiles() : writtenFiles;
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
        artifactPublisher.writeJson(state, "validation", "generated-code-compile-result.json", result);
    }
}
