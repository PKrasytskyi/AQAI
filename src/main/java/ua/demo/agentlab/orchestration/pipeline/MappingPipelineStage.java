package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

public record MappingPipelineStage(
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        UiTestPlan uiTestPlan,
        MappedUiKnowledge mappedUiKnowledge,
        FlowScopedKnowledgePackage flowScopedKnowledgePackage,
        PageKnowledgeCacheLookupResult pageKnowledgeCacheLookupResult,
        List<PageModelEnrichmentRecord> pageModelEnrichments,
        MappedUiKnowledge enrichedMappedUiKnowledge,
        List<AssertionContract> assertionContracts,
        KnowledgeRunMetadata knowledgeRunMetadata
) {
    public MappingPipelineStage {
        pageModelEnrichments = pageModelEnrichments == null ? List.of() : List.copyOf(pageModelEnrichments);
        assertionContracts = assertionContracts == null ? List.of() : List.copyOf(assertionContracts);
    }
}
