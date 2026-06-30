package ua.demo.agentlab.api.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.persistence.GeneratedFileWriter;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;
import java.util.Set;

public class ApiGeneratedSourcePersistenceAgent implements WorkflowAgent,
        PipelineAgent<ApiGenerationResult, List<String>> {

    private final GeneratedFileWriter generatedFileWriter;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public ApiGeneratedSourcePersistenceAgent(GeneratedFileWriter generatedFileWriter) {
        if (generatedFileWriter == null) {
            throw new IllegalArgumentException("generatedFileWriter cannot be null");
        }
        this.generatedFileWriter = generatedFileWriter;
    }

    @Override
    public String name() {
        return "api-generated-source-persistence-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.API_GENERATION_RESULT);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.WRITTEN_FILES);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.API_GENERATION_RESULT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.WRITTEN_FILES;
    }

    @Override
    public ApiGenerationResult inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (store == null) {
            throw new IllegalStateException("API generation result is not available from artifact store");
        }
        return store.require(WorkflowArtifact.API_GENERATION_RESULT);
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        if (store == null || state == null
                || "true".equalsIgnoreCase(state.getArtifacts().get("api.generated.source.persisted"))) {
            return false;
        }
        return store.get(WorkflowArtifact.API_GENERATION_RESULT)
                .filter(ApiGenerationResult.class::isInstance)
                .map(ApiGenerationResult.class::cast)
                .map(this::isPersistable)
                .orElse(false);
    }

    @Override
    public List<String> execute(ApiGenerationResult input, WorkflowRunEnvelope run) {
        List<GeneratedSourceFile> files = input == null ? List.of() : input.sourceFiles();
        for (GeneratedSourceFile file : files) {
            generatedFileWriter.write(file);
        }
        return files.stream().map(GeneratedSourceFile::relativePath).toList();
    }

    @Override
    public void applyOutput(List<String> writtenFiles, WorkflowState state) {
        publisher.publishApiGeneratedSourcePersistence(writtenFiles, state);
    }

    private boolean isPersistable(ApiGenerationResult result) {
        return result != null
                && result.qualityReport() != null
                && !result.qualityReport().hasBlockingIssues()
                && !result.sourceFiles().isEmpty();
    }
}
