package ua.demo.agentlab.ui.discovery.interaction.agent;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.*;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.catalog.CanonicalConfirmedUiCatalogAssembler;
import ua.demo.agentlab.ui.discovery.catalog.CanonicalLocatorCandidateCoverageAssembler;
import ua.demo.agentlab.ui.discovery.interaction.observability.EvidenceProjectionTraceAssembler;
import ua.demo.agentlab.ui.discovery.interaction.persistence.CanonicalInteractionGraphProjectionWriter;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceAssembler;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

import java.util.List;
import java.util.Set;

/** Consolidated canonical decision stage. Catalog, DB, coverage and trace are projections of one bundle. */
public final class UiInteractionEvidenceAgent implements WorkflowAgent,
        PipelineAgent<UiInteractionEvidenceInput, UiInteractionEvidenceOutput> {

    private final CanonicalInteractionEvidenceAssembler canonicalAssembler = new CanonicalInteractionEvidenceAssembler();
    private final CanonicalLocatorCandidateCoverageAssembler coverageAssembler =
            new CanonicalLocatorCandidateCoverageAssembler();
    private final CanonicalConfirmedUiCatalogAssembler catalogAssembler = new CanonicalConfirmedUiCatalogAssembler();
    private final CanonicalInteractionGraphProjectionWriter graphWriter =
            new CanonicalInteractionGraphProjectionWriter(new PropertiesNeo4jRuntimeConfig());
    private final EvidenceProjectionTraceAssembler traceAssembler = new EvidenceProjectionTraceAssembler();
    private final AiArtifactPublisher publisher = new AiArtifactPublisher();

    @Override public String name() { return "ui-interaction-evidence-agent"; }

    @Override public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.UI_EFFECTIVE_INTERACTION_INVENTORY,
                WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION,
                WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS,
                WorkflowArtifact.ASSERTION_CONTRACTS);
    }

    @Override public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.CANONICAL_INTERACTION_EVIDENCE,
                WorkflowArtifact.LOCATOR_CANDIDATE_COVERAGE_REPORT,
                WorkflowArtifact.CONFIRMED_UI_CATALOG,
                WorkflowArtifact.INTERACTION_GRAPH_PROJECTION,
                WorkflowArtifact.EVIDENCE_PROJECTION_TRACE);
    }

    @Override public WorkflowArtifact input() { return WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION; }
    @Override public WorkflowArtifact output() { return WorkflowArtifact.CANONICAL_INTERACTION_EVIDENCE; }

    @Override @SuppressWarnings("unchecked")
    public UiInteractionEvidenceInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        List<StructuredBehaviorContract> requirements = store.require(WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS);
        List<AssertionContract> assertions = store.require(WorkflowArtifact.ASSERTION_CONTRACTS);
        return new UiInteractionEvidenceInput(store.require(WorkflowArtifact.UI_EFFECTIVE_INTERACTION_INVENTORY),
                store.require(WorkflowArtifact.SPA_LIVE_TARGETED_VERIFICATION), requirements, assertions);
    }

    @Override public boolean supports(UiInteractionEvidenceInput input, WorkflowRunEnvelope run) {
        return input != null && input.inventory() != null && input.liveVerification() != null;
    }

    @Override public UiInteractionEvidenceOutput execute(UiInteractionEvidenceInput input, WorkflowRunEnvelope run) {
        var canonical = canonicalAssembler.assemble(input.inventory(), input.liveVerification(), input.requirements());
        var coverage = coverageAssembler.assemble(canonical);
        var catalog = catalogAssembler.assemble(canonical, input.assertions());
        var graph = graphWriter.persist(canonical, input.inventory());
        var trace = traceAssembler.assemble(canonical, catalog, graph);
        return new UiInteractionEvidenceOutput(canonical, coverage, catalog, graph, trace);
    }

    @Override public void applyOutput(UiInteractionEvidenceOutput output, WorkflowState state) {
        if (output == null || state == null) return;
        state.addArtifact("canonical.interactions.candidates", String.valueOf(output.canonical().candidates().size()));
        state.addArtifact("canonical.interactions.confirmed", String.valueOf(output.projectionTrace().confirmed()));
        state.addArtifact("interaction.graph.projection.executed", String.valueOf(output.graphProjection().executed()));
        state.addArtifact("confirmed.ui.catalog.complete", String.valueOf(output.catalog().complete()));
        publisher.writeJson(state, "quality", "canonical-interaction-evidence.json", output.canonical());
        publisher.writeJson(state, "quality", "locator-candidate-coverage.json", output.coverage());
        publisher.writeJson(state, "quality", "confirmed-ui-catalog.json", output.catalog());
        publisher.writeJson(state, "quality", "evidence-projection-trace.json", output.projectionTrace());
        publisher.writeJson(state, "quality", "interaction-graph-projection.json", output.graphProjection());
    }
}
