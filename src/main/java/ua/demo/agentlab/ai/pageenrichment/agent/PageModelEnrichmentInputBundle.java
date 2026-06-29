package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;

public record PageModelEnrichmentInputBundle(
        ProjectProfile projectProfile,
        UiTestPlan uiTestPlan,
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        PageModelBundle pageModelBundle,
        MappedUiKnowledge mappedUiKnowledge,
        FlowScopedKnowledgePackage flowScopedKnowledgePackage,
        PageKnowledgeCacheLookupResult cacheLookupResult
) {
}
