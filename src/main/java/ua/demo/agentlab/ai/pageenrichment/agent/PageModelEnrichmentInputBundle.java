package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;

import java.util.List;

public record PageModelEnrichmentInputBundle(
        ProjectProfile projectProfile,
        UiTestPlan uiTestPlan,
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        PageModelBundle pageModelBundle,
        MappedUiKnowledge mappedUiKnowledge,
        FlowScopedKnowledgePackage flowScopedKnowledgePackage,
        PageKnowledgeCacheLookupResult cacheLookupResult,
        List<BoundSpaBehaviorContract> finalizedBehaviorBindings
) {
    public PageModelEnrichmentInputBundle(
            ProjectProfile projectProfile,
            UiTestPlan uiTestPlan,
            CanonicalTestCaseBundle canonicalTestCaseBundle,
            PageModelBundle pageModelBundle,
            MappedUiKnowledge mappedUiKnowledge,
            FlowScopedKnowledgePackage flowScopedKnowledgePackage,
            PageKnowledgeCacheLookupResult cacheLookupResult
    ) {
        this(projectProfile, uiTestPlan, canonicalTestCaseBundle, pageModelBundle, mappedUiKnowledge,
                flowScopedKnowledgePackage, cacheLookupResult, null);
    }
}
