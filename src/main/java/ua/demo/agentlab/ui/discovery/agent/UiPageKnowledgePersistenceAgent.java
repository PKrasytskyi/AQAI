package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.LocatorPromotionFilter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeNamespaceEnricher;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeWriteResult;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeWriter;

import java.util.List;
import java.util.Set;

public class UiPageKnowledgePersistenceAgent implements WorkflowAgent,
        PipelineAgent<UiKnowledgePersistenceInput, UiKnowledgePersistenceOutput> {

    private final List<PageKnowledgeWriter> writers;
    private final KnowledgeNamespaceEnricher namespaceEnricher = new KnowledgeNamespaceEnricher();
    private final LocatorPromotionFilter locatorPromotionFilter = new LocatorPromotionFilter();
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public UiPageKnowledgePersistenceAgent(List<PageKnowledgeWriter> writers) {
        if (writers == null || writers.isEmpty()) {
            throw new IllegalArgumentException("writers cannot be null or empty");
        }
        this.writers = List.copyOf(writers);
    }

    @Override
    public String name() {
        return "ui-page-knowledge-persistence-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.ENRICHED_MAPPED_UI_KNOWLEDGE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.UI_KNOWLEDGE_PERSISTED);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.ENRICHED_MAPPED_UI_KNOWLEDGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_KNOWLEDGE_PERSISTED;
    }

    @Override
    public UiKnowledgePersistenceInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        KnowledgeRunMetadata runMetadata = state.getKnowledgeRunMetadata();
        if (runMetadata == null) {
            runMetadata = KnowledgeRunMetadata.from(state, name());
        }
        return new UiKnowledgePersistenceInput(
                state.getMappedUiKnowledge(),
                state.getEnrichedMappedUiKnowledge(),
                state.getFlowScopedKnowledgePackage(),
                runMetadata
        );
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return state != null
                && selectedKnowledge(new UiKnowledgePersistenceInput(
                        state.getMappedUiKnowledge(),
                        state.getEnrichedMappedUiKnowledge(),
                        state.getFlowScopedKnowledgePackage(),
                        state.getKnowledgeRunMetadata()
                )) != null
                && !state.getArtifacts().containsKey("ui.knowledge.persistence.completed");
    }

    @Override
    public UiKnowledgePersistenceOutput execute(UiKnowledgePersistenceInput input, WorkflowRunEnvelope run) {
        MappedUiKnowledge promotedKnowledge = locatorPromotionFilter.filterForPersistence(selectedKnowledge(input));
        MappedUiKnowledge namespacedKnowledge = namespaceEnricher.enrich(promotedKnowledge, input.runMetadata());
        List<PageKnowledgeWriteResult> results = new java.util.ArrayList<>();
        for (PageKnowledgeWriter writer : writers) {
            PageKnowledgeWriteResult result = writeSafely(writer, namespacedKnowledge);
            results.add(result);
        }
        return new UiKnowledgePersistenceOutput(results, input.runMetadata());
    }

    @Override
    public void applyOutput(UiKnowledgePersistenceOutput output, WorkflowState state) {
        if (output == null) {
            return;
        }
        outputPublisher.publishKnowledgePersistence(output.results(), state, output.runMetadata());
    }

    private MappedUiKnowledge selectedKnowledge(UiKnowledgePersistenceInput input) {
        var knowledge = input.enrichedMappedUiKnowledge();
        if (knowledge == null && input.flowScopedKnowledgePackage() != null) {
            knowledge = input.flowScopedKnowledgePackage().mappedUiKnowledge();
        }
        return knowledge == null ? input.mappedUiKnowledge() : knowledge;
    }

    private PageKnowledgeWriteResult writeSafely(PageKnowledgeWriter writer, MappedUiKnowledge knowledge) {
        try {
            return writer.write(knowledge);
        } catch (Exception exception) {
            String target = writer == null ? "unknown" : writer.getClass().getSimpleName();
            return new PageKnowledgeWriteResult(
                    target,
                    false,
                    0,
                    0,
                    0,
                    "Persistence skipped after failure: " + exception.getMessage()
            );
        }
    }
}
