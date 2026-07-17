package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;

import java.util.List;

public record SpaInventoryInput(
        PageModelBundle pageModels,
        MappedUiKnowledge mappedKnowledge,
        KnowledgeRunMetadata metadata,
        CanonicalTestCaseBundle canonicalTestCases,
        List<StructuredBehaviorContract> structuredContracts
) {
    public SpaInventoryInput(
            PageModelBundle pageModels,
            MappedUiKnowledge mappedKnowledge,
            KnowledgeRunMetadata metadata,
            CanonicalTestCaseBundle canonicalTestCases
    ) {
        this(pageModels, mappedKnowledge, metadata, canonicalTestCases, List.of());
    }
}
