package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.spa.LiveTargetedVerificationArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.LiveTargetedVerificationRunner;
import ua.demo.agentlab.ui.discovery.spa.LiveTransitionDiscoveryArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.LiveTransitionDiscoveryService;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.SpaStateGraphArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.SpaStateGraphWriter;
import ua.demo.agentlab.ui.discovery.spa.StructuredBehaviorExecutionArtifactWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

import java.util.List;
import java.util.Set;

public class UiLiveSpaTargetedVerificationAgent implements WorkflowAgent,
        PipelineAgent<SpaLiveTargetedVerificationInput, SpaLiveTargetedVerificationOutput> {
    private final PropertiesSpaInventoryConfig config;
    private final LiveTargetedVerificationRunner runner;
    private final LiveTargetedVerificationArtifactWriter artifactWriter;
    private final StructuredBehaviorExecutionArtifactWriter structuredBehaviorArtifactWriter;
    private final LiveTransitionDiscoveryService transitionDiscoveryService;
    private final LiveTransitionDiscoveryArtifactWriter transitionDiscoveryArtifactWriter;
    private final SpaStateGraphWriter stateGraphWriter;
    private final SpaStateGraphArtifactWriter stateGraphArtifactWriter;

    public UiLiveSpaTargetedVerificationAgent(PropertiesSpaInventoryConfig config,
                                               LiveTargetedVerificationRunner runner,
                                               LiveTargetedVerificationArtifactWriter artifactWriter) {
        this.config = config == null ? new PropertiesSpaInventoryConfig() : config;
        this.runner = runner == null ? new LiveTargetedVerificationRunner() : runner;
        this.artifactWriter = artifactWriter == null ? new LiveTargetedVerificationArtifactWriter() : artifactWriter;
        this.structuredBehaviorArtifactWriter = new StructuredBehaviorExecutionArtifactWriter();
        this.transitionDiscoveryService = new LiveTransitionDiscoveryService();
        this.transitionDiscoveryArtifactWriter = new LiveTransitionDiscoveryArtifactWriter();
        this.stateGraphWriter = new SpaStateGraphWriter(new PropertiesNeo4jRuntimeConfig());
        this.stateGraphArtifactWriter = new SpaStateGraphArtifactWriter();
    }

    @Override public String name() { return "ui-live-spa-targeted-verification-agent"; }
    @Override public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.PROJECT_PROFILE, WorkflowArtifact.UI_INTERACTION_INVENTORY,
                WorkflowArtifact.SPA_TARGETED_VERIFICATION, WorkflowArtifact.SPA_COMPONENT_INTERACTION_GRAPH,
                WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS);
    }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION,
            WorkflowArtifact.SPA_LIVE_TRANSITION_DISCOVERY,
            WorkflowArtifact.SPA_STRUCTURED_BEHAVIOR_EXECUTION, WorkflowArtifact.SPA_EVIDENCE_LIFECYCLE); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.SPA_TARGETED_VERIFICATION; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION; }
    @Override public SpaLiveTargetedVerificationInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new SpaLiveTargetedVerificationInput(store.require(WorkflowArtifact.PROJECT_PROFILE),
                store.require(WorkflowArtifact.UI_INTERACTION_INVENTORY), store.require(WorkflowArtifact.SPA_TARGETED_VERIFICATION),
                store.require(WorkflowArtifact.SPA_COMPONENT_INTERACTION_GRAPH),
                store.require(WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS));
    }
    @Override public boolean supports(SpaLiveTargetedVerificationInput input, WorkflowRunEnvelope run) {
        return input != null && input.profile() != null && input.inventory() != null && input.planned() != null;
    }
    @Override public SpaLiveTargetedVerificationOutput execute(SpaLiveTargetedVerificationInput input, WorkflowRunEnvelope run) {
        var result = runner.verify(input.profile(), input.inventory(), input.planned(), input.interactionGraph(), config.load());
        var transitionDiscovery = transitionDiscoveryService.discover(input.sourceBindings(), result);
        var lifecycle = ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceLifecycleResult.skipped(
                "Canonical interaction projection owns evidence promotion and persistence");
        var behavior = new ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionBundle(
                ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionBundle.SCHEMA_VERSION,
                result.runMetadata(), List.of(), List.of("awaiting-target-state-binding"));
        String promotion = "skipped:awaiting-target-state-binding";
        String statePersistence = stateGraphWriter.persist(result.stateGraph());
        return new SpaLiveTargetedVerificationOutput(result, transitionDiscovery, lifecycle, behavior, promotion, statePersistence,
                List.of(artifactWriter.write(result), transitionDiscoveryArtifactWriter.write(transitionDiscovery),
                        stateGraphArtifactWriter.write(result.stateGraph()),
                        structuredBehaviorArtifactWriter.write(behavior)));
    }

    @Override public void applyOutput(SpaLiveTargetedVerificationOutput output, WorkflowState state) {
        if (state == null || output == null) return;
        state.addArtifact("spa.live.verification.executed", String.valueOf(output.result().executed()));
        state.addArtifact("spa.live.verification.passed", String.valueOf(output.result().passed()));
        state.addArtifact("spa.live.verification.locators", String.valueOf(output.result().locatorVerifications().size()));
        state.addArtifact("spa.live.verification.actions", String.valueOf(output.result().actionVerifications().size()));
        state.addArtifact("spa.live.verification.locators.verified.count", String.valueOf(output.result().locatorVerifications().stream()
                .filter(ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification::verified).count()));
        state.addArtifact("spa.live.verification.actions.verified.count", String.valueOf(output.result().actionVerifications().stream()
                .filter(ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification::verified).count()));
        state.addArtifact("spa.live.verification.states", String.valueOf(output.result().stateGraph().states().size()));
        state.addArtifact("spa.live.verification.transitions", String.valueOf(output.result().stateGraph().transitions().size()));
        state.addArtifact("spa.live.transition.discovery.confirmed", String.valueOf(output.transitionDiscovery().transitions().stream()
                .filter(ua.demo.agentlab.ui.discovery.spa.model.RequirementStateTransition::confirmed).count()));
        state.addArtifact("spa.live.verification.state-graph.persistence", output.stateGraphPersistence());
        state.addArtifact("spa.structured.behavior.executions", String.valueOf(output.behaviorExecution().results().size()));
        state.addArtifact("spa.structured.behavior.promotion", output.behaviorPromotion());
        state.addArtifact("spa.live.verification.artifacts", String.join(",", output.artifacts()));
    }
}
