package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.SpaEvidenceRetentionGraphWriter;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceRetentionResult;

import java.util.Set;

public class UiSpaEvidenceRetentionAgent implements WorkflowAgent,
        PipelineAgent<SpaEvidenceRetentionInput, SpaEvidenceRetentionResult> {
    private final PropertiesSpaInventoryConfig config;
    private final SpaEvidenceRetentionGraphWriter writer;

    public UiSpaEvidenceRetentionAgent(PropertiesSpaInventoryConfig config, SpaEvidenceRetentionGraphWriter writer) {
        this.config = config == null ? new PropertiesSpaInventoryConfig() : config;
        this.writer = writer;
    }
    @Override public String name() { return "ui-spa-evidence-retention-agent"; }
    @Override public Set<WorkflowArtifact> requires() { return Set.of(WorkflowArtifact.UI_INTERACTION_INVENTORY); }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.SPA_EVIDENCE_RETENTION); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.UI_INTERACTION_INVENTORY; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.SPA_EVIDENCE_RETENTION; }
    @Override public SpaEvidenceRetentionInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new SpaEvidenceRetentionInput(store.require(WorkflowArtifact.UI_INTERACTION_INVENTORY));
    }
    @Override public boolean supports(SpaEvidenceRetentionInput input, WorkflowRunEnvelope run) {
        return input != null && input.inventory() != null && !input.inventory().pages().isEmpty();
    }
    @Override public SpaEvidenceRetentionResult execute(SpaEvidenceRetentionInput input, WorkflowRunEnvelope run) {
        return writer == null ? SpaEvidenceRetentionResult.skipped("SPA retention writer is unavailable")
                : writer.apply(input.inventory().pages().get(0).runMetadata(), config.load());
    }

    @Override public void applyOutput(SpaEvidenceRetentionResult output, WorkflowState state) {
        if (state == null || output == null) return;
        state.addArtifact("spa.retention.executed", String.valueOf(output.executed()));
        state.addArtifact("spa.retention.degraded.retired", String.valueOf(output.degradedRetired()));
        state.addArtifact("spa.retention.orphan.retired", String.valueOf(output.orphanRetired()));
        state.addArtifact("spa.retention.details", output.details());
    }
}
