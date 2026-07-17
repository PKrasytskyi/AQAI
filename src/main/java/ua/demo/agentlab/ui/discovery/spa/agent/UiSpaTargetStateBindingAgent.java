package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.spa.StructuredBehaviorBindingArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.StructuredBehaviorNeedsReviewWriter;
import ua.demo.agentlab.ui.discovery.spa.TargetStateBindingArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.TargetStateBindingService;
import ua.demo.agentlab.ui.discovery.spa.LiveTargetInventoryAssembler;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.SourceStateBindingService;
import ua.demo.agentlab.ui.discovery.spa.LiveTargetMappingArtifactWriter;

import java.util.List;
import java.util.Set;

public final class UiSpaTargetStateBindingAgent implements WorkflowAgent,
        PipelineAgent<SpaTargetStateBindingInput, SpaTargetStateBindingOutput> {
    private final TargetStateBindingService service = new TargetStateBindingService();
    private final TargetStateBindingArtifactWriter targetWriter = new TargetStateBindingArtifactWriter();
    private final StructuredBehaviorBindingArtifactWriter behaviorWriter = new StructuredBehaviorBindingArtifactWriter();
    private final LiveTargetInventoryAssembler targetInventoryAssembler = new LiveTargetInventoryAssembler();
    private final SourceStateBindingService sourceBindingService = new SourceStateBindingService();
    private final PropertiesSpaInventoryConfig config = new PropertiesSpaInventoryConfig();
    private final LiveTargetMappingArtifactWriter mappingArtifactWriter = new LiveTargetMappingArtifactWriter();

    @Override public String name() { return "ui-spa-target-state-binding-agent"; }
    @Override public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.PROJECT_PROFILE, WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS,
                WorkflowArtifact.SPA_PAGE_INVENTORY, WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS,
                WorkflowArtifact.SPA_LIVE_TRANSITION_DISCOVERY, WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION);
    }
    @Override public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.SPA_TARGET_STATE_BINDINGS, WorkflowArtifact.SPA_STRUCTURED_BEHAVIOR_BINDINGS,
                WorkflowArtifact.SPA_EFFECTIVE_PAGE_INVENTORY, WorkflowArtifact.SPA_REBOUND_SOURCE_STATE_BINDINGS);
    }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.SPA_LIVE_TRANSITION_DISCOVERY; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.SPA_TARGET_STATE_BINDINGS; }

    @Override public SpaTargetStateBindingInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new SpaTargetStateBindingInput(store.require(WorkflowArtifact.PROJECT_PROFILE),
                store.require(WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS),
                store.require(WorkflowArtifact.SPA_PAGE_INVENTORY),
                store.require(WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS),
                store.require(WorkflowArtifact.SPA_LIVE_TRANSITION_DISCOVERY),
                store.require(WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION));
    }

    @Override public boolean supports(SpaTargetStateBindingInput input, WorkflowRunEnvelope run) {
        return input != null
                && input.profile() != null
                && input.inventory() != null
                && input.sources() != null
                && input.transitionDiscovery() != null
                && input.liveVerification() != null;
    }

    @Override public SpaTargetStateBindingOutput execute(SpaTargetStateBindingInput input, WorkflowRunEnvelope run) {
        var inventoryConfig = config.load();
        var effectiveInventory = targetInventoryAssembler.merge(input.profile(), input.inventory(),
                input.liveVerification().targetPageSnapshots(), input.transitionDiscovery().runMetadata(), inventoryConfig);
        var reboundSources = sourceBindingService.bind(input.profile(), input.contracts(), effectiveInventory, inventoryConfig);
        reboundSources = sourceBindingService.rebindConfirmedTransitions(
                reboundSources, input.transitionDiscovery(), effectiveInventory);
        var result = service.bind(input.contracts(), effectiveInventory, reboundSources, input.transitionDiscovery());
        List<String> artifacts = new java.util.ArrayList<>(mappingArtifactWriter.write(
                input.liveVerification().targetPageSnapshots(), effectiveInventory, reboundSources));
        artifacts.add(targetWriter.write(result));
        artifacts.add(behaviorWriter.write(result.behaviorBindings()));
        artifacts.add(new StructuredBehaviorNeedsReviewWriter().write(result.behaviorBindings()));
        return new SpaTargetStateBindingOutput(result, effectiveInventory, reboundSources,
                artifacts);
    }

    @Override public void applyOutput(SpaTargetStateBindingOutput output, WorkflowState state) {
        if (state == null || output == null || output.bindings() == null) return;
        state.addArtifact("spa.target.state.bindings", String.valueOf(output.bindings().targets().size()));
        state.addArtifact("spa.target.state.confirmed", String.valueOf(output.bindings().targets().stream()
                .filter(target -> target.transitionConfirmed()).count()));
    }
}
