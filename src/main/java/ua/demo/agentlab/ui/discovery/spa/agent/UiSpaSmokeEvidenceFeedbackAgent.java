package ua.demo.agentlab.ui.discovery.spa.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.interaction.persistence.CanonicalInteractionSmokeFeedbackWriter;
import ua.demo.agentlab.ui.discovery.spa.model.SpaSmokeEvidenceFeedbackResult;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ui.generation.provenance.PomSourceMap;
import ua.demo.agentlab.validation.smoke.LiveUiSmokeResult;
import ua.demo.agentlab.validation.feedback.GeneratedUiClosedLoopPolicy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Bridges live smoke output to the exact locator/action ids that the deterministic POM writer used. */
public class UiSpaSmokeEvidenceFeedbackAgent implements WorkflowAgent,
        PipelineAgent<SpaSmokeEvidenceFeedbackInput, SpaSmokeEvidenceFeedbackResult> {
    private final PropertiesSpaInventoryConfig config;
    private final CanonicalInteractionSmokeFeedbackWriter writer;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final GeneratedUiClosedLoopPolicy closedLoopPolicy = new GeneratedUiClosedLoopPolicy();

    public UiSpaSmokeEvidenceFeedbackAgent(PropertiesSpaInventoryConfig config, CanonicalInteractionSmokeFeedbackWriter writer) {
        this.config = config == null ? new PropertiesSpaInventoryConfig() : config;
        this.writer = writer;
    }
    @Override public String name() { return "ui-spa-smoke-evidence-feedback-agent"; }
    @Override public Set<WorkflowArtifact> requires() { return Set.of(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES,
            WorkflowArtifact.GENERATED_SOURCE_MANIFEST, WorkflowArtifact.UI_INTERACTION_INVENTORY,
            WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION, WorkflowArtifact.COMPILE_RESULT,
            WorkflowArtifact.REVIEW_RESULT, WorkflowArtifact.GENERATED_UI_SMOKE_RESULT); }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.SPA_SMOKE_EVIDENCE_FEEDBACK); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.SPA_SMOKE_EVIDENCE_FEEDBACK; }
    @Override @SuppressWarnings("unchecked") public SpaSmokeEvidenceFeedbackInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new SpaSmokeEvidenceFeedbackInput((List<GeneratedSourceFile>) store.require(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES),
                store.require(WorkflowArtifact.UI_INTERACTION_INVENTORY), store.require(WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION),
                store.require(WorkflowArtifact.COMPILE_RESULT), store.require(WorkflowArtifact.REVIEW_RESULT),
                store.require(WorkflowArtifact.GENERATED_UI_SMOKE_RESULT), readLiveSmoke());
    }
    @Override public boolean supports(SpaSmokeEvidenceFeedbackInput input, WorkflowRunEnvelope run) { return input != null && !input.sources().isEmpty(); }
    @Override public SpaSmokeEvidenceFeedbackResult execute(SpaSmokeEvidenceFeedbackInput input, WorkflowRunEnvelope run) {
        var decision = closedLoopPolicy.evaluate(input.compileResult(), input.reviewReport(), input.generatedSmoke(), input.liveSmoke());
        if (!decision.eligibleForDbFeedback()) {
            SpaSmokeEvidenceFeedbackResult result = SpaSmokeEvidenceFeedbackResult.skipped(
                    "Closed-loop DB feedback skipped: " + decision.reason());
            writeArtifact(result);
            return result;
        }
        PomSourceMap sourceMap = readSourceMap();
        var liveVerification = new ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult(
                input.verification().schemaVersion(), input.verification().runMetadata(),
                input.verification().locatorVerifications(), input.verification().actionVerifications(),
                List.of(), input.verification().sourceTrace());
        SpaSmokeEvidenceFeedbackResult result = writer == null ? SpaSmokeEvidenceFeedbackResult.skipped("SPA smoke feedback writer is unavailable")
                : writer.write(decision.passed(), sourceMap, input.inventory(), liveVerification, config.load());
        writeArtifact(result);
        return result;
    }

    private PomSourceMap readSourceMap() {
        Path artifact = Path.of("target", "ai-run", "validation", "pom-source-map.json");
        if (!Files.isRegularFile(artifact)) return new PomSourceMap(0, List.of());
        try { return mapper.readValue(artifact.toFile(), PomSourceMap.class); }
        catch (Exception ignored) { return new PomSourceMap(0, List.of()); }
    }

    private LiveUiSmokeResult readLiveSmoke() {
        Path artifact = Path.of("target", "ai-run", "validation", "live-ui-smoke-result.json");
        if (!Files.isRegularFile(artifact)) return null;
        try { return mapper.readValue(artifact.toFile(), LiveUiSmokeResult.class); }
        catch (Exception ignored) { return null; }
    }
    private void writeArtifact(SpaSmokeEvidenceFeedbackResult result) {
        try {
            Path directory = Path.of("target", "ai-run", "validation"); Files.createDirectories(directory);
            mapper.writeValue(directory.resolve("spa-smoke-evidence-feedback.json").toFile(), result);
        } catch (Exception ignored) { /* Feedback must never make a completed test run fail. */ }
    }

    @Override public void applyOutput(SpaSmokeEvidenceFeedbackResult output, WorkflowState state) {
        if (state == null || output == null) return;
        state.addArtifact("spa.smoke.feedback.executed", String.valueOf(output.executed()));
        state.addArtifact("spa.smoke.feedback.live.passed", String.valueOf(output.liveSmokePassed()));
        state.addArtifact("spa.smoke.feedback.locators", String.valueOf(output.locatorsLinked()));
        state.addArtifact("spa.smoke.feedback.actions", String.valueOf(output.actionsLinked()));
        state.addArtifact("spa.smoke.feedback.details", output.details());
    }
}
