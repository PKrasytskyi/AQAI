package ua.demo.agentlab.artifactreuse.agent;

import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.artifactreuse.lifecycle.ArtifactLifecyclePromotionService;
import ua.demo.agentlab.artifactreuse.lifecycle.ArtifactLifecycleResult;
import ua.demo.agentlab.artifactreuse.model.RunRecord;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArtifactLifecyclePromotionAgent implements WorkflowAgent,
        PipelineAgent<ArtifactLifecyclePromotionService.ArtifactLifecycleInput, ArtifactLifecycleResult> {

    private final ArtifactLifecyclePromotionService promotionService;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public ArtifactLifecyclePromotionAgent(ArtifactLifecyclePromotionService promotionService) {
        if (promotionService == null) {
            throw new IllegalArgumentException("promotionService cannot be null");
        }
        this.promotionService = promotionService;
    }

    @Override
    public String name() {
        return "artifact-lifecycle-promotion-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.POM_CONTRACT_SPECS,
                WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES,
                WorkflowArtifact.COMPILE_RESULT,
                WorkflowArtifact.REVIEW_RESULT,
                WorkflowArtifact.GENERATED_UI_SMOKE_RESULT
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.ARTIFACT_LIFECYCLE_RESULT);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.GENERATED_UI_SMOKE_RESULT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.ARTIFACT_LIFECYCLE_RESULT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArtifactLifecyclePromotionService.ArtifactLifecycleInput inputFrom(
            PipelineArtifactStore store,
            WorkflowState state
    ) {
        return new ArtifactLifecyclePromotionService.ArtifactLifecycleInput(
                (List<PomContractSpec>) store.get(WorkflowArtifact.POM_CONTRACT_SPECS).orElse(List.of()),
                (List<GeneratedSourceFile>) store.get(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES)
                        .orElse(state.getPageObjectFiles()),
                (GeneratedCodeValidationResult) store.get(WorkflowArtifact.COMPILE_RESULT)
                        .orElse(state.getGeneratedCodeValidationResult()),
                (GeneratedCodeReviewReport) store.get(WorkflowArtifact.REVIEW_RESULT)
                        .orElse(state.getGeneratedCodeReviewReport()),
                (GeneratedUiSmokeResult) store.get(WorkflowArtifact.GENERATED_UI_SMOKE_RESULT).orElse(null),
                state.getArtifacts(),
                runRecord(state, runMetadata(state))
        );
    }

    @Override
    public boolean supports(ArtifactLifecyclePromotionService.ArtifactLifecycleInput input, WorkflowRunEnvelope run) {
        return input != null && !input.contracts().isEmpty() && input.smokeResult() != null;
    }

    @Override
    public ArtifactLifecycleResult execute(
            ArtifactLifecyclePromotionService.ArtifactLifecycleInput input,
            WorkflowRunEnvelope run
    ) {
        return promotionService.promote(input);
    }

    @Override
    public void applyOutput(ArtifactLifecycleResult output, WorkflowState state) {
        if (state == null || output == null) {
            return;
        }
        state.addArtifact("artifact.lifecycle.evaluated.count", String.valueOf(output.artifactsEvaluated()));
        state.addArtifact("artifact.lifecycle.stable.count", String.valueOf(output.stableArtifacts()));
        state.addArtifact("artifact.lifecycle.needs-review.count", String.valueOf(output.needsReviewArtifacts()));
        output.entries().forEach(entry -> {
            String prefix = "artifact.lifecycle." + fileStem(entry.pageName()) + ".";
            state.addArtifact(prefix + "status", entry.status().name());
            state.addArtifact(prefix + "reusable", String.valueOf(entry.reusable()));
            state.addArtifact(prefix + "qualityScore", String.valueOf(entry.qualityScore()));
            state.addArtifact(prefix + "reason", entry.reason());
            state.addArtifact(prefix + "registry.updated", String.valueOf(entry.registryUpdated()));
        });
        artifactPublisher.writeJson(state, "validation", "artifact-lifecycle-result.json", output);
        state.addFinding("Artifact lifecycle: " + output.stableArtifacts() + " stable, "
                + output.needsReviewArtifacts() + " needs review");
    }

    private RunRecord runRecord(WorkflowState state, KnowledgeRunMetadata metadata) {
        if (metadata != null) {
            return new RunRecord(metadata.runId(), metadata.appId(), metadata.baseUrlHash(),
                    metadata.requirementSetHash(), metadata.discoverySessionId(), metadata.schemaVersion(),
                    metadata.createdAt(), name());
        }
        String runId = state == null || state.runEnvelope() == null ? "" : state.runEnvelope().runMetadata().runId();
        String createdAt = state == null || state.runEnvelope() == null
                ? Instant.now().toString()
                : state.runEnvelope().runMetadata().createdAt().toString();
        return new RunRecord(runId, "", "", "", "", "", createdAt, name());
    }

    private KnowledgeRunMetadata runMetadata(WorkflowState state) {
        return state == null ? null : state.getKnowledgeRunMetadata();
    }

    private String fileStem(String value) {
        String safe = value == null ? "" : value.trim();
        return (safe.isBlank() ? "Page" : safe).replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
