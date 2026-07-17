package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryBuilder;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryGraphWriter;
import ua.demo.agentlab.ui.discovery.spa.TypedComponentFlowArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.TypedComponentFlowBuilder;
import ua.demo.agentlab.ui.discovery.spa.TypedComponentFlowGraphWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

import java.util.List;
import java.util.Set;

public class UiSpaInventoryAgent implements WorkflowAgent, PipelineAgent<SpaInventoryInput, SpaInventoryOutput> {

    private final PropertiesSpaInventoryConfig config;
    private final SpaInventoryBuilder inventoryBuilder;
    private final SpaInventoryArtifactWriter artifactWriter;
    private final SpaInventoryGraphWriter graphWriter;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public UiSpaInventoryAgent(
            PropertiesSpaInventoryConfig config,
            SpaInventoryBuilder inventoryBuilder,
            SpaInventoryArtifactWriter artifactWriter,
            SpaInventoryGraphWriter graphWriter
    ) {
        this.config = config == null ? new PropertiesSpaInventoryConfig() : config;
        this.inventoryBuilder = inventoryBuilder == null ? new SpaInventoryBuilder() : inventoryBuilder;
        this.artifactWriter = artifactWriter == null ? new SpaInventoryArtifactWriter() : artifactWriter;
        this.graphWriter = graphWriter;
    }

    @Override
    public String name() {
        return "ui-spa-inventory-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.PAGE_MODEL_BUNDLE,
                WorkflowArtifact.MAPPED_UI_KNOWLEDGE,
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.SPA_PAGE_INVENTORY, WorkflowArtifact.SPA_INVENTORY_PERSISTENCE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.MAPPED_UI_KNOWLEDGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.SPA_PAGE_INVENTORY;
    }

    @Override
    public SpaInventoryInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new SpaInventoryInput(
                store.require(WorkflowArtifact.PAGE_MODEL_BUNDLE),
                store.require(WorkflowArtifact.MAPPED_UI_KNOWLEDGE),
                KnowledgeRunMetadata.from(state, name()),
                store.require(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE),
                store.require(WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS)
        );
    }

    @Override
    public boolean supports(SpaInventoryInput input, WorkflowRunEnvelope run) {
        return input != null && input.pageModels() != null && input.mappedKnowledge() != null
                && input.canonicalTestCases() != null;
    }

    @Override
    public SpaInventoryOutput execute(SpaInventoryInput input, WorkflowRunEnvelope run) {
        var inventory = inventoryBuilder.build(
                input.pageModels(), input.mappedKnowledge(), input.metadata(), config.load(),
                input.canonicalTestCases(), input.structuredContracts());
        String artifact = artifactWriter.write(inventory);
        var typedFlows = new TypedComponentFlowBuilder().build(inventory);
        String flowArtifact = new TypedComponentFlowArtifactWriter().write(typedFlows);
        new TypedComponentFlowGraphWriter(new PropertiesNeo4jRuntimeConfig()).persist(typedFlows);
        var persistence = graphWriter == null
                ? ua.demo.agentlab.ui.discovery.spa.SpaInventoryPersistenceResult.skipped("SPA inventory graph writer is unavailable")
                : graphWriter.persist(inventory);
        return new SpaInventoryOutput(inventory, persistence, List.of(artifact, flowArtifact));
    }

    @Override
    public void applyOutput(SpaInventoryOutput output, WorkflowState state) {
        outputPublisher.publishSpaInventory(output, state);
    }
}
