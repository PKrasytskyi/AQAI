package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

public record UiKnowledgePersistenceInput(
        MappedUiKnowledge mappedUiKnowledge,
        MappedUiKnowledge enrichedMappedUiKnowledge,
        FlowScopedKnowledgePackage flowScopedKnowledgePackage,
        KnowledgeRunMetadata runMetadata
) {
}
