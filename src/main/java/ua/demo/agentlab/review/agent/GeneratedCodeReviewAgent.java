package ua.demo.agentlab.review.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.review.GeneratedCodeReviewer;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.persistence.GeneratedSourceManifest;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;
import java.util.Set;

public class GeneratedCodeReviewAgent implements WorkflowAgent,
        PipelineAgent<GeneratedCodeReviewAgent.Input, GeneratedCodeReviewReport> {

    private final GeneratedCodeReviewer reviewer;
    private final StageOutputPublisher publisher = new StageOutputPublisher();
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public GeneratedCodeReviewAgent(GeneratedCodeReviewer reviewer){
        this.reviewer = reviewer;
    }

    @Override
    public String name() {
        return "generated-code-review-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.GENERATED_SOURCE_MANIFEST,
                WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES,
                WorkflowArtifact.GENERATED_UI_TEST_SOURCES,
                WorkflowArtifact.COMPILE_RESULT
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.REVIEW_RESULT, WorkflowArtifact.GENERATED_CODE_REVIEW);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.GENERATED_SOURCE_MANIFEST;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.REVIEW_RESULT;
    }

    @Override
    public Input inputFrom(PipelineArtifactStore store, WorkflowState state) {
        GeneratedSourceManifest manifest = store.require(WorkflowArtifact.GENERATED_SOURCE_MANIFEST);
        List<GeneratedSourceFile> pageObjectFiles = store.getList(
                WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES,
                GeneratedSourceFile.class
        );
        if (pageObjectFiles.isEmpty()) {
            pageObjectFiles = store.getList(
                    WorkflowArtifact.PAGE_OBJECT_FILES,
                    GeneratedSourceFile.class
            );
        }
        if (pageObjectFiles.isEmpty()) {
            pageObjectFiles = state.getPageObjectFiles();
        }
        List<GeneratedSourceFile> uiTestFiles = store.getList(
                WorkflowArtifact.GENERATED_UI_TEST_SOURCES,
                GeneratedSourceFile.class
        );
        if (uiTestFiles.isEmpty()) {
            uiTestFiles = state.getUiTestFiles();
        }
        GeneratedUiSources ownedSources = manifest.selectOwned(new GeneratedUiSources(
                pageObjectFiles,
                uiTestFiles
        ));
        if (ownedSources.allFiles().size() != manifest.files().size()) {
            throw new IllegalStateException("Current-run source manifest does not match generated source content");
        }
        return new Input(manifest, ownedSources);
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return state != null
                && state.getGeneratedCodeReviewReport() == null
                && !inputFrom(store, state).sources().isEmpty();
    }

    @Override
    public GeneratedCodeReviewReport execute(Input input, WorkflowRunEnvelope run) {
        return reviewer.review(input.sources());
    }

    @Override
    public void applyOutput(GeneratedCodeReviewReport report, WorkflowState state) {
        publisher.publishGeneratedCodeReview(report, state);
        artifactPublisher.writeJson(state, "validation", "generated-code-review-result.json", report);
    }

    public record Input(GeneratedSourceManifest manifest, GeneratedUiSources sources) {
        public Input {
            if (manifest == null) {
                throw new IllegalArgumentException("manifest cannot be null");
            }
            sources = sources == null ? new GeneratedUiSources(null, null) : sources;
        }
    }
}
