package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

public record PageKnowledgeCacheLookupInput(
        UiTestPlan uiTestPlan,
        MappedUiKnowledge mappedUiKnowledge,
        FlowScopedKnowledgePackage flowScopedKnowledgePackage,
        KnowledgeRunMetadata runMetadata
) {
}
