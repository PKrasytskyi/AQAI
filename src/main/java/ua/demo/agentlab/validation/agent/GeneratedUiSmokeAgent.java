package ua.demo.agentlab.validation.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeService;
import ua.demo.agentlab.validation.smoke.LiveLoginDashboardSmokeService;
import ua.demo.agentlab.validation.smoke.LiveUiSmokeResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class GeneratedUiSmokeAgent implements WorkflowAgent,
        PipelineAgent<GeneratedUiSmokeAgent.Input, GeneratedUiSmokeResult> {

    private final GeneratedUiSmokeService smokeService;
    private final LiveLoginDashboardSmokeService liveSmokeService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public GeneratedUiSmokeAgent(GeneratedUiSmokeService smokeService) {
        this.smokeService = smokeService == null ? new GeneratedUiSmokeService() : smokeService;
        this.liveSmokeService = new LiveLoginDashboardSmokeService();
    }

    @Override
    public String name() {
        return "generated-ui-smoke-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES,
                WorkflowArtifact.PERSISTED_GENERATED_SOURCES,
                WorkflowArtifact.COMPILE_RESULT,
                WorkflowArtifact.REVIEW_RESULT
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATED_UI_SMOKE_RESULT);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_UI_SMOKE_RESULT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Input inputFrom(PipelineArtifactStore store, WorkflowState state) {
        List<GeneratedSourceFile> pageObjects = store == null
                ? state.getPageObjectFiles()
                : (List<GeneratedSourceFile>) store.get(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES)
                .or(() -> store.get(WorkflowArtifact.PAGE_OBJECT_FILES))
                .orElse(state.getPageObjectFiles());
        List<GeneratedSourceFile> tests = store == null
                ? state.getUiTestFiles()
                : (List<GeneratedSourceFile>) store.get(WorkflowArtifact.UI_TEST_FILES).orElse(state.getUiTestFiles());
        List<String> persisted = store == null
                ? state.getWrittenFiles()
                : (List<String>) store.get(WorkflowArtifact.PERSISTED_GENERATED_SOURCES)
                .or(() -> store.get(WorkflowArtifact.WRITTEN_FILES))
                .orElse(state.getWrittenFiles());
        GeneratedCodeValidationResult compile = store == null
                ? state.getGeneratedCodeValidationResult()
                : (GeneratedCodeValidationResult) store.get(WorkflowArtifact.COMPILE_RESULT)
                .orElse(state.getGeneratedCodeValidationResult());
        GeneratedCodeReviewReport review = store == null
                ? state.getGeneratedCodeReviewReport()
                : (GeneratedCodeReviewReport) store.get(WorkflowArtifact.REVIEW_RESULT)
                .orElse(state.getGeneratedCodeReviewReport());
        return new Input(new GeneratedUiSources(pageObjects, tests), persisted, compile, review);
    }

    @Override
    public boolean supports(Input input, WorkflowRunEnvelope run) {
        return input != null
                && input.compileResult() != null
                && input.reviewReport() != null
                && !input.sources().pageObjectFiles().isEmpty();
    }

    @Override
    public GeneratedUiSmokeResult execute(Input input, WorkflowRunEnvelope run) {
        return smokeService.smoke(input.sources(), input.persistedFiles(), input.compileResult(), input.reviewReport());
    }

    @Override
    public void applyOutput(GeneratedUiSmokeResult output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        state.addArtifact("generated.ui.smoke.status", output.status().name());
        state.addArtifact("generated.ui.smoke.files.checked", String.valueOf(output.filesChecked()));
        state.addArtifact("generated.ui.smoke.issue.count", String.valueOf(output.issues().size()));
        writeSmokeArtifact(output, state);
        LiveUiSmokeResult liveSmoke = liveSmokeService.smoke(new GeneratedUiSources(
                state.getPageObjectFiles(),
                state.getUiTestFiles()
        ));
        writeLiveSmokeArtifact(liveSmoke, state);
        state.addFinding(output.summary());
        state.addFinding(liveSmoke.summary());
        if (!output.passed()) {
            state.fail("Generated UI smoke validation failed: " + output.summary());
        }
        if (liveSmoke.failed()) {
            state.fail("Generated UI live smoke validation failed: " + liveSmoke.summary());
        }
    }

    private void writeSmokeArtifact(GeneratedUiSmokeResult output, WorkflowState state) {
        try {
            Path outputDir = Path.of("target", "ai-run", "validation");
            Files.createDirectories(outputDir);
            Path artifact = outputDir.resolve("generated-ui-smoke-result.json");
            objectMapper.writeValue(artifact.toFile(), output);
            state.addArtifact("generated.ui.smoke.artifact", artifact.toString());
        } catch (Exception exception) {
            state.addFinding("Failed to write generated UI smoke artifact: " + exception.getMessage());
        }
    }

    private void writeLiveSmokeArtifact(LiveUiSmokeResult output, WorkflowState state) {
        try {
            Path outputDir = Path.of("target", "ai-run", "validation");
            Files.createDirectories(outputDir);
            Path artifact = outputDir.resolve("live-ui-smoke-result.json");
            objectMapper.writeValue(artifact.toFile(), output);
            state.addArtifact("generated.ui.live.smoke.status", output.status().name());
            state.addArtifact("generated.ui.live.smoke.issue.count", String.valueOf(output.issues().size()));
            state.addArtifact("generated.ui.live.smoke.artifact", artifact.toString());
        } catch (Exception exception) {
            state.addFinding("Failed to write live UI smoke artifact: " + exception.getMessage());
        }
    }

    public record Input(
            GeneratedUiSources sources,
            List<String> persistedFiles,
            GeneratedCodeValidationResult compileResult,
            GeneratedCodeReviewReport reviewReport
    ) {
        public Input {
            sources = sources == null ? new GeneratedUiSources(List.of(), List.of()) : sources;
            persistedFiles = persistedFiles == null ? List.of() : List.copyOf(persistedFiles);
        }
    }
}
