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
        PipelineAgent<GeneratedSourcePersistenceInput, GeneratedSourceManifest> {

    private final GeneratedFileWriter generatedFileWriter;
    private final GeneratedPageSourceReconciler pageSourceReconciler;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public LocalFilePersistenceAgent(GeneratedFileWriter generatedFileWriter) {
        this(generatedFileWriter, new GeneratedPageSourceReconciler());
    }

    LocalFilePersistenceAgent(
            GeneratedFileWriter generatedFileWriter,
            GeneratedPageSourceReconciler pageSourceReconciler
    ) {
        if (generatedFileWriter == null || pageSourceReconciler == null) {
            throw new IllegalArgumentException("persistence dependencies cannot be null");
        }
        this.generatedFileWriter = generatedFileWriter;
        this.pageSourceReconciler = pageSourceReconciler;
    }

    @Override
    public String name() {
        return "file-persistence-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES,
                WorkflowArtifact.GENERATED_UI_TEST_SOURCES,
                WorkflowArtifact.PROJECT_PROFILE
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.GENERATED_SOURCE_MANIFEST,
                WorkflowArtifact.PERSISTED_GENERATED_SOURCES,
                WorkflowArtifact.WRITTEN_FILES
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_SOURCE_MANIFEST;
    }

    @Override
    public GeneratedSourcePersistenceInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        List<GeneratedSourceFile> pageObjectFiles = store.getList(
                WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES,
                GeneratedSourceFile.class
        );
        if (pageObjectFiles.isEmpty()) {
            pageObjectFiles = store.getList(WorkflowArtifact.PAGE_OBJECT_FILES, GeneratedSourceFile.class);
        }
        List<GeneratedSourceFile> testFiles = store.getList(
                WorkflowArtifact.GENERATED_UI_TEST_SOURCES,
                GeneratedSourceFile.class
        );
        if (testFiles.isEmpty()) {
            testFiles = store.getList(WorkflowArtifact.UI_TEST_FILES, GeneratedSourceFile.class);
        }
        return new GeneratedSourcePersistenceInput(
                store.require(WorkflowArtifact.PROJECT_PROFILE),
                new GeneratedUiSources(pageObjectFiles, testFiles)
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
        return !inputFrom(store, state).sources().isEmpty();
    }

    @Override
    public GeneratedSourceManifest execute(GeneratedSourcePersistenceInput input, WorkflowRunEnvelope run) {
        GeneratedUiSources sources = input.sources();
        pageSourceReconciler.removeStaleGeneratedSources(sources.allFiles());
        for (GeneratedSourceFile file : sources.allFiles()) {
            generatedFileWriter.write(file);
        }
        return GeneratedSourceManifest.create(input.projectProfile(), run, sources);
    }

    @Override
    public void applyOutput(GeneratedSourceManifest manifest, WorkflowState state) {
        if (manifest == null) {
            return;
        }
        List<String> persisted = manifest.persistedPaths();
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
                "generated-source-manifest.json",
                manifest
        );
        artifactPublisher.writeJson(
                state,
                "validation",
                "persisted-generated-sources.json",
                new PersistedGeneratedSourcesArtifact(
                        manifest.runId(),
                        manifest.namespaceId(),
                        writtenCount,
                        persisted
                )
        );
    }

    private record PersistedGeneratedSourcesArtifact(
            String runId,
            String namespaceId,
            int filesWritten,
            List<String> files
    ) { }
}
