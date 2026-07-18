package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.SpaEvidenceNeedsReviewWriter;
import ua.demo.agentlab.ui.discovery.spa.SpaTargetedVerificationArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.SpaTargetedVerificationPlanner;

import java.util.ArrayList;
import java.util.Set;

public class UiSpaTargetedVerificationAgent implements WorkflowAgent,
        PipelineAgent<SpaTargetedVerificationInput, SpaTargetedVerificationOutput> {

    private final PropertiesSpaInventoryConfig config;
    private final SpaTargetedVerificationPlanner planner;
    private final SpaTargetedVerificationArtifactWriter artifactWriter;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public UiSpaTargetedVerificationAgent(
            PropertiesSpaInventoryConfig config,
            SpaTargetedVerificationPlanner planner,
            SpaTargetedVerificationArtifactWriter artifactWriter
    ) {
        this.config = config == null ? new PropertiesSpaInventoryConfig() : config;
        this.planner = planner == null ? new SpaTargetedVerificationPlanner() : planner;
        this.artifactWriter = artifactWriter == null ? new SpaTargetedVerificationArtifactWriter() : artifactWriter;
    }

    @Override
    public String name() { return "ui-spa-targeted-verification-agent"; }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.SPA_PAGE_INVENTORY, WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS, WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.SPA_TARGETED_VERIFICATION, WorkflowArtifact.SPA_EVIDENCE_LIFECYCLE);
    }

    @Override
    public WorkflowArtifact input() { return WorkflowArtifact.SPA_PAGE_INVENTORY; }

    @Override
    public WorkflowArtifact output() { return WorkflowArtifact.SPA_TARGETED_VERIFICATION; }

    @Override
    public SpaTargetedVerificationInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new SpaTargetedVerificationInput(
                store.require(WorkflowArtifact.SPA_PAGE_INVENTORY),
                store.require(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE),
                store.require(WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS),
                store.require(WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS)
        );
    }

    @Override
    public boolean supports(SpaTargetedVerificationInput input, WorkflowRunEnvelope run) {
        return input != null && input.inventory() != null && input.testCases() != null;
    }

    @Override
    public SpaTargetedVerificationOutput execute(SpaTargetedVerificationInput input, WorkflowRunEnvelope run) {
        var effectiveConfig = config.load();
        var verification = planner.verify(input.inventory(), input.testCases(), input.structuredContracts(),
                input.sourceBindings(), effectiveConfig);
        // Promotion is deliberately deferred to the fresh-browser verification stage. Planner output
        // is based on discovery evidence and must never become stable knowledge by itself.
        var lifecycle = ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceLifecycleResult
                .skipped("awaiting-live-targeted-verification");
        var artifacts = new ArrayList<>(artifactWriter.write(verification, lifecycle));
        artifacts.add(new SpaEvidenceNeedsReviewWriter().write(input.inventory(), verification));
        return new SpaTargetedVerificationOutput(verification, lifecycle, artifacts);
    }

    @Override
    public void applyOutput(SpaTargetedVerificationOutput output, WorkflowState state) {
        outputPublisher.publishSpaTargetedVerification(output, state);
    }
}
