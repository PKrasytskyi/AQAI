package ua.demo.agentlab.validation.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.validation.GeneratedUiContractValidationResult;
import ua.demo.agentlab.validation.GeneratedUiContractValidator;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;
import java.util.Set;

public class GeneratedUiContractValidationAgent implements WorkflowAgent,
        PipelineAgent<GeneratedUiSources, GeneratedUiContractValidationResult> {

    private final GeneratedUiContractValidator validator;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public GeneratedUiContractValidationAgent(GeneratedUiContractValidator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("validator cannot be null");
        }
        this.validator = validator;
    }

    @Override
    public String name() {
        return "generated-ui-contract-validation-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.PAGE_OBJECT_FILES, WorkflowArtifact.UI_TEST_FILES);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATED_UI_CONTRACT_VALIDATION);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.PAGE_OBJECT_FILES;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_UI_CONTRACT_VALIDATION;
    }

    @Override
    public GeneratedUiSources inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (store == null) {
            return new GeneratedUiSources(state.getPageObjectFiles(), state.getUiTestFiles());
        }
        List<GeneratedSourceFile> pageObjectFiles = store.getList(
                WorkflowArtifact.PAGE_OBJECT_FILES,
                GeneratedSourceFile.class
        );
        List<GeneratedSourceFile> uiTestFiles = store.getList(
                WorkflowArtifact.UI_TEST_FILES,
                GeneratedSourceFile.class
        );
        return new GeneratedUiSources(
                pageObjectFiles.isEmpty() ? state.getPageObjectFiles() : pageObjectFiles,
                uiTestFiles.isEmpty() ? state.getUiTestFiles() : uiTestFiles
        );
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return state != null
                && state.getGeneratedUiContractValidationResult() == null
                && !inputFrom(store, state).pageObjectFiles().isEmpty()
                && !inputFrom(store, state).uiTestFiles().isEmpty();
    }

    @Override
    public GeneratedUiContractValidationResult execute(GeneratedUiSources input, WorkflowRunEnvelope run) {
        return validator.validate(input);
    }

    @Override
    public void applyOutput(GeneratedUiContractValidationResult result, WorkflowState state) {
        publisher.publishGeneratedUiContractValidation(result, state);
    }
}
