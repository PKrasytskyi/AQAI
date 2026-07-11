package ua.demo.agentlab.persistence;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;
import java.util.Set;

public class LocalFilePersistenceAgent implements WorkflowAgent,
        PipelineAgent<GeneratedUiSources, List<String>> {

    private final GeneratedFileWriter generatedFileWriter;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public LocalFilePersistenceAgent(GeneratedFileWriter generatedFileWriter){
        this.generatedFileWriter = generatedFileWriter;
    }

    @Override
    public String name() {
        return "file-persistence-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.PERSISTED_GENERATED_SOURCES, WorkflowArtifact.WRITTEN_FILES);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.PERSISTED_GENERATED_SOURCES;
    }

    @Override
    public GeneratedUiSources inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new GeneratedUiSources(
                store.<java.util.List<GeneratedSourceFile>>get(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES)
                        .or(() -> store.<java.util.List<GeneratedSourceFile>>get(WorkflowArtifact.PAGE_OBJECT_FILES))
                        .map(value -> (java.util.List<GeneratedSourceFile>) value)
                        .orElse(List.of()),
                store.<java.util.List<GeneratedSourceFile>>get(WorkflowArtifact.UI_TEST_FILES)
                        .map(value -> (java.util.List<GeneratedSourceFile>) value)
                        .orElse(List.of())
        );
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        if (store == null || state == null) {
            return false;
        }
        if ("true".equalsIgnoreCase(state.getArtifacts().get("generated.file.persisted"))) {
            return false;
        }
        return !inputFrom(store, state).isEmpty();
    }

    @Override
    public List<String> execute(GeneratedUiSources input, WorkflowRunEnvelope run) {
        List<String> written = new java.util.ArrayList<>();
        for (GeneratedSourceFile file : input.allFiles()) {
            generatedFileWriter.write(file);
            written.add(file.relativePath());
        }
        return written;
    }

    @Override
    public void applyOutput(List<String> paths, WorkflowState state) {
        List<String> persisted = paths == null ? List.of() : List.copyOf(paths);
        for (String path : persisted) {
            state.addWrittenFile(path);
        }
        int writtenCount = persisted.size();
        state.addArtifact("generated.file.persisted", "true");
        state.addArtifact("generated.file.written", String.valueOf(writtenCount));
        state.addFinding("Generated file persisted: " + writtenCount);
        artifactPublisher.writeJson(
                state,
                "validation",
                "persisted-generated-sources.json",
                new PersistedGeneratedSourcesArtifact(writtenCount, persisted)
        );
    }

    private record PersistedGeneratedSourcesArtifact(
            int filesWritten,
            List<String> files
    ) {
    }
}
