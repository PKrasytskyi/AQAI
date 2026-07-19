package ua.demo.agentlab.ui.discovery.interaction.inventory.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventoryArtifactWriter;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventoryBuilder;
import ua.demo.agentlab.ui.discovery.spa.TypedComponentFlowArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.TypedComponentFlowBuilder;
import ua.demo.agentlab.ui.discovery.spa.TypedComponentFlowGraphWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

import java.util.List;
import java.util.Set;

public class UiInteractionInventoryAgent implements WorkflowAgent, PipelineAgent<UiInteractionInventoryInput, UiInteractionInventoryOutput> {

    private final PropertiesSpaInventoryConfig config;
    private final UiInteractionInventoryBuilder inventoryBuilder;
    private final UiInteractionInventoryArtifactWriter artifactWriter;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public UiInteractionInventoryAgent(
            PropertiesSpaInventoryConfig config,
            UiInteractionInventoryBuilder inventoryBuilder,
            UiInteractionInventoryArtifactWriter artifactWriter
    ) {
        this.config = config == null ? new PropertiesSpaInventoryConfig() : config;
        this.inventoryBuilder = inventoryBuilder == null ? new UiInteractionInventoryBuilder() : inventoryBuilder;
        this.artifactWriter = artifactWriter == null ? new UiInteractionInventoryArtifactWriter() : artifactWriter;
    }

    @Override
    public String name() {
        return "ui-interaction-inventory-agent";
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
        return Set.of(WorkflowArtifact.UI_INTERACTION_INVENTORY, WorkflowArtifact.UI_INTERACTION_INVENTORY_PERSISTENCE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.MAPPED_UI_KNOWLEDGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_INTERACTION_INVENTORY;
    }

    @Override
    public UiInteractionInventoryInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new UiInteractionInventoryInput(
                store.require(WorkflowArtifact.PAGE_MODEL_BUNDLE),
                store.require(WorkflowArtifact.MAPPED_UI_KNOWLEDGE),
                KnowledgeRunMetadata.from(state, name()),
                store.require(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE),
                store.require(WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS)
        );
    }

    @Override
    public boolean supports(UiInteractionInventoryInput input, WorkflowRunEnvelope run) {
        return input != null && input.pageModels() != null && input.mappedKnowledge() != null
                && input.canonicalTestCases() != null;
    }

    @Override
    public UiInteractionInventoryOutput execute(UiInteractionInventoryInput input, WorkflowRunEnvelope run) {
        var inventory = inventoryBuilder.build(
                input.pageModels(), input.mappedKnowledge(), input.metadata(), config.load(),
                input.canonicalTestCases(), input.structuredContracts());
        String artifact = artifactWriter.write(inventory);
        var typedFlows = new TypedComponentFlowBuilder().build(inventory);
        String flowArtifact = new TypedComponentFlowArtifactWriter().write(typedFlows);
        new TypedComponentFlowGraphWriter(new PropertiesNeo4jRuntimeConfig()).persist(typedFlows);
        var persistence = ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventoryPersistenceResult.skipped(
                "Canonical interaction projection owns locator/action persistence");
        return new UiInteractionInventoryOutput(inventory, persistence, List.of(artifact, flowArtifact));
    }

    @Override
    public void applyOutput(UiInteractionInventoryOutput output, WorkflowState state) {
        outputPublisher.publishUiInteractionInventory(output, state);
    }
}
