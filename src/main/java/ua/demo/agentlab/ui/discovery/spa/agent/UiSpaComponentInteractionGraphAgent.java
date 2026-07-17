package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.spa.ComponentInteractionGraphArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.ComponentInteractionGraphBuilder;
import ua.demo.agentlab.ui.discovery.spa.ComponentInteractionGraphWriter;

import java.util.List;
import java.util.Set;

public class UiSpaComponentInteractionGraphAgent implements WorkflowAgent,
        PipelineAgent<SpaComponentInteractionGraphInput, SpaComponentInteractionGraphOutput> {
    private final ComponentInteractionGraphBuilder builder;
    private final ComponentInteractionGraphWriter graphWriter;
    private final ComponentInteractionGraphArtifactWriter artifactWriter;

    public UiSpaComponentInteractionGraphAgent(ComponentInteractionGraphBuilder builder,
                                               ComponentInteractionGraphWriter graphWriter,
                                               ComponentInteractionGraphArtifactWriter artifactWriter) {
        this.builder = builder == null ? new ComponentInteractionGraphBuilder() : builder;
        this.graphWriter = graphWriter;
        this.artifactWriter = artifactWriter == null ? new ComponentInteractionGraphArtifactWriter() : artifactWriter;
    }

    @Override public String name() { return "ui-spa-component-interaction-graph-agent"; }
    @Override public Set<WorkflowArtifact> requires() { return Set.of(WorkflowArtifact.SPA_PAGE_INVENTORY); }
    @Override public Set<WorkflowArtifact> produces() { return Set.of(WorkflowArtifact.SPA_COMPONENT_INTERACTION_GRAPH); }
    @Override public WorkflowArtifact input() { return WorkflowArtifact.SPA_PAGE_INVENTORY; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.SPA_COMPONENT_INTERACTION_GRAPH; }
    @Override public SpaComponentInteractionGraphInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new SpaComponentInteractionGraphInput(store.require(WorkflowArtifact.SPA_PAGE_INVENTORY));
    }
    @Override public boolean supports(SpaComponentInteractionGraphInput input, WorkflowRunEnvelope run) {
        return input != null && input.inventory() != null;
    }
    @Override public SpaComponentInteractionGraphOutput execute(SpaComponentInteractionGraphInput input, WorkflowRunEnvelope run) {
        var graph = builder.build(input.inventory());
        return new SpaComponentInteractionGraphOutput(graph,
                graphWriter == null ? "skipped:graph-writer-unavailable" : graphWriter.persist(graph),
                List.of(artifactWriter.write(graph)));
    }

    @Override public void applyOutput(SpaComponentInteractionGraphOutput output, WorkflowState state) {
        if (state == null || output == null) return;
        state.addArtifact("spa.component.interaction.dependencies", String.valueOf(output.graph().dependencies().size()));
        state.addArtifact("spa.component.interaction.persistence", output.persistenceDetails());
        state.addArtifact("spa.component.interaction.artifacts", String.join(",", output.artifacts()));
    }
}
