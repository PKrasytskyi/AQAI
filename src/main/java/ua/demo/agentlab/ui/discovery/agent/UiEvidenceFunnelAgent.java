package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.ui.discovery.evidence.funnel.*;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.interaction.observability.EvidenceProjectionTrace;

import java.util.List;
import java.util.Set;

/** Acceptance boundary that makes every requirement's evidence outcome explicit before POM generation. */
public final class UiEvidenceFunnelAgent implements WorkflowAgent,
        PipelineAgent<UiEvidenceFunnelInput, UiEvidenceFunnelReport> {

    private final UiEvidenceFunnelAssembler assembler;
    private final AiArtifactPublisher artifactPublisher;

    public UiEvidenceFunnelAgent() {
        this(new UiEvidenceFunnelAssembler(), new AiArtifactPublisher());
    }

    UiEvidenceFunnelAgent(
            UiEvidenceFunnelAssembler assembler,
            AiArtifactPublisher artifactPublisher
    ) {
        this.assembler = assembler;
        this.artifactPublisher = artifactPublisher;
    }

    @Override
    public String name() {
        return "ui-evidence-funnel-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS,
                WorkflowArtifact.UI_EFFECTIVE_INTERACTION_INVENTORY,
                WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS,
                WorkflowArtifact.SPA_LIVE_TRANSITION_DISCOVERY,
                WorkflowArtifact.SPA_STRUCTURED_BEHAVIOR_BINDINGS,
                WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION,
                WorkflowArtifact.SPA_TARGET_STATE_BINDINGS,
                WorkflowArtifact.AI_CONTEXT_PACKAGE,
                WorkflowArtifact.LOCATOR_CANDIDATE_COVERAGE_REPORT,
                WorkflowArtifact.CONFIRMED_UI_CATALOG,
                WorkflowArtifact.EVIDENCE_PROJECTION_TRACE
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.UI_EVIDENCE_FUNNEL_REPORT);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.AI_CONTEXT_PACKAGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_EVIDENCE_FUNNEL_REPORT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public UiEvidenceFunnelInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        List<StructuredBehaviorContract> requirements = store.require(WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS);
        UiInteractionInventory inventory = store.require(WorkflowArtifact.UI_EFFECTIVE_INTERACTION_INVENTORY);
        SourceStateBindingBundle sourceStateBindings = store.require(WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS);
        LiveTransitionDiscovery liveTransitionDiscovery = store.require(WorkflowArtifact.SPA_LIVE_TRANSITION_DISCOVERY);
        List<BoundSpaBehaviorContract> bindings = store.require(WorkflowArtifact.SPA_STRUCTURED_BEHAVIOR_BINDINGS);
        SpaLiveTargetedVerificationResult verification = store.require(WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION);
        TargetStateBindingBundle targetStateBindings = store.require(WorkflowArtifact.SPA_TARGET_STATE_BINDINGS);
        AiContextPackage aiContext = store.require(WorkflowArtifact.AI_CONTEXT_PACKAGE);
        EvidenceProjectionTrace projectionTrace = store.require(WorkflowArtifact.EVIDENCE_PROJECTION_TRACE);
        String runId = state == null || state.runEnvelope() == null
                ? ""
                : state.runEnvelope().runMetadata().runId();
        return new UiEvidenceFunnelInput(runId, requirements, inventory, sourceStateBindings,
                liveTransitionDiscovery, bindings, verification, targetStateBindings, aiContext, projectionTrace);
    }

    @Override
    public boolean supports(UiEvidenceFunnelInput input, WorkflowRunEnvelope run) {
        return input != null && input.inventory() != null && input.liveVerification() != null && input.aiContext() != null;
    }

    @Override
    public UiEvidenceFunnelReport execute(UiEvidenceFunnelInput input, WorkflowRunEnvelope run) {
        try {
            UiEvidenceFunnelReport report = assembler.assemble(input);
            return report;
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage().replaceAll("\\s+", " ").trim();
            throw new IllegalStateException("Cannot build UI evidence funnel: " + message, exception);
        }
    }

    @Override
    public void applyOutput(UiEvidenceFunnelReport report, WorkflowState state) {
        if (state == null || report == null) {
            return;
        }
        UiEvidenceFunnelMetrics metrics = report.metrics();
        state.addArtifact("ui.evidence.funnel.completeness.passed", String.valueOf(report.completenessPassed()));
        state.addArtifact("ui.evidence.funnel.pom.readiness.passed", String.valueOf(report.pomReadinessPassed()));
        state.addArtifact("ui.evidence.funnel.requirements", String.valueOf(report.requirements().size()));
        state.addArtifact("ui.evidence.funnel.raw.locator.candidates", String.valueOf(metrics.rawLocatorCandidates()));
        state.addArtifact("ui.evidence.funnel.live.verified.locators", String.valueOf(metrics.liveVerifiedLocators()));
        state.addArtifact("ui.evidence.funnel.db.stable.locators", String.valueOf(metrics.dbStableLocators()));
        state.addArtifact("ui.evidence.funnel.context.prompt.allowed.locators",
                String.valueOf(metrics.contextPromptAllowedLocators()));
        state.addArtifact("ui.evidence.funnel.requirement.scoped.prompt.allowed.locators",
                String.valueOf(metrics.requirementScopedPromptAllowedLocators()));
        state.addArtifact("ui.evidence.funnel.requirement.bound.pages", String.valueOf(metrics.requirementBoundPages()));
        state.addArtifact("ui.evidence.funnel.prompt.eligible.pages", String.valueOf(metrics.promptEligiblePages()));
        state.addFinding("UI evidence funnel completeness=" + report.completenessPassed()
                + ", POM readiness=" + report.pomReadinessPassed()
                + ", POM-eligible pages=" + metrics.promptEligiblePages());
        artifactPublisher.writeJson(state, "quality", "ui-evidence-funnel.json", report);
    }
}
