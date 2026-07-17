package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.SourceStateBindingArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.SourceStateBindingService;

import java.util.List;
import java.util.Set;

public final class UiSpaSourceStateBindingAgent implements WorkflowAgent,
        PipelineAgent<SpaSourceStateBindingInput, SpaSourceStateBindingOutput> {
    private final SourceStateBindingService service;
    private final PropertiesSpaInventoryConfig config;
    private final SourceStateBindingArtifactWriter writer;

    public UiSpaSourceStateBindingAgent() {
        this(new SourceStateBindingService(), new PropertiesSpaInventoryConfig(), new SourceStateBindingArtifactWriter());
    }

    UiSpaSourceStateBindingAgent(SourceStateBindingService service, PropertiesSpaInventoryConfig config,
                                 SourceStateBindingArtifactWriter writer) {
        this.service = service;
        this.config = config;
        this.writer = writer;
    }

    @Override public String name() { return "ui-spa-source-state-binding-agent"; }
    @Override public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.PROJECT_PROFILE, WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS,
                WorkflowArtifact.SPA_PAGE_INVENTORY);
    }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.SPA_SOURCE_STATE_BINDINGS; }

    @Override public SpaSourceStateBindingInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new SpaSourceStateBindingInput(store.require(WorkflowArtifact.PROJECT_PROFILE),
                store.require(WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS),
                store.require(WorkflowArtifact.SPA_PAGE_INVENTORY));
    }

    @Override public boolean supports(SpaSourceStateBindingInput input, WorkflowRunEnvelope run) {
        return input != null && input.profile() != null && input.inventory() != null;
    }

    @Override public SpaSourceStateBindingOutput execute(SpaSourceStateBindingInput input, WorkflowRunEnvelope run) {
        var result = service.bind(input.profile(), input.contracts(), input.inventory(), config.load());
        return new SpaSourceStateBindingOutput(result, List.of(writer.write(result)));
    }

    @Override public void applyOutput(SpaSourceStateBindingOutput output, WorkflowState state) {
        if (state == null || output == null || output.bindings() == null) return;
        state.addArtifact("spa.source.state.bindings", String.valueOf(output.bindings().bindings().size()));
        state.addArtifact("spa.source.state.eligible", String.valueOf(output.bindings().bindings().stream()
                .filter(binding -> binding.liveVerificationEligible()).count()));
    }
}
