package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

import java.util.List;

public record AiContextPackage(
        String objective,
        NormalizedRequirementBundle normalizedRequirementBundle,
        GenerationPolicy generationPolicy,
        ProjectProfile projectProfile,
        TestPlan testPlan,
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        UiTestPlan uiTestPlan,
        CanonicalPageFlowModel canonicalPageFlowModel,
        MappedUiKnowledge mappedUiKnowledge,
        MappedUiKnowledgeCurated mappedUiKnowledgeCurated,
        PageModelBundle pageModelBundle,
        CanonicalUiInteractionModel canonicalInteractionModel,
        UiKnowledgeRetrievalContext retrievalContext,
        List<AssertionContract> assertionContracts,
        List<PageModelEnrichmentRecord> pageModelEnrichments,
        List<String> templateCapabilities,
        List<PromptLocatorEvidence> dbStableLocatorEvidence,
        List<PromptLocatorEvidence> confirmedCatalogLocatorEvidence,
        PromptUiEvidence promptUiEvidence
) {
    public AiContextPackage {
        objective = objective == null ? "" : objective.trim();
        canonicalInteractionModel = canonicalInteractionModel == null
                ? CanonicalUiInteractionModel.empty()
                : canonicalInteractionModel;
        retrievalContext = retrievalContext == null
                ? UiKnowledgeRetrievalContext.empty("Retrieval context is not available")
                : retrievalContext;
        pageModelBundle = pageModelBundle == null ? new PageModelBundle(List.of()) : pageModelBundle;
        assertionContracts = assertionContracts == null ? List.of() : List.copyOf(assertionContracts);
        pageModelEnrichments = pageModelEnrichments == null ? List.of() : List.copyOf(pageModelEnrichments);
        templateCapabilities = templateCapabilities == null ? List.of() : List.copyOf(templateCapabilities);
        dbStableLocatorEvidence = dbStableLocatorEvidence == null ? List.of() : List.copyOf(dbStableLocatorEvidence);
        confirmedCatalogLocatorEvidence = confirmedCatalogLocatorEvidence == null
                ? List.of()
                : List.copyOf(confirmedCatalogLocatorEvidence);
        promptUiEvidence = promptUiEvidence == null ? PromptUiEvidence.empty("prompt-evidence:not-built") : promptUiEvidence;
    }

    public AiContextPackage(
            String objective,
            NormalizedRequirementBundle normalizedRequirementBundle,
            GenerationPolicy generationPolicy,
            ProjectProfile projectProfile,
            TestPlan testPlan,
            CanonicalTestCaseBundle canonicalTestCaseBundle,
            UiTestPlan uiTestPlan,
            CanonicalPageFlowModel canonicalPageFlowModel,
            MappedUiKnowledge mappedUiKnowledge,
            MappedUiKnowledgeCurated mappedUiKnowledgeCurated,
            PageModelBundle pageModelBundle,
            CanonicalUiInteractionModel canonicalInteractionModel,
            UiKnowledgeRetrievalContext retrievalContext,
            List<AssertionContract> assertionContracts,
            List<PageModelEnrichmentRecord> pageModelEnrichments,
            List<String> templateCapabilities,
            List<PromptLocatorEvidence> dbStableLocatorEvidence,
            PromptUiEvidence promptUiEvidence
    ) {
        this(
                objective,
                normalizedRequirementBundle,
                generationPolicy,
                projectProfile,
                testPlan,
                canonicalTestCaseBundle,
                uiTestPlan,
                canonicalPageFlowModel,
                mappedUiKnowledge,
                mappedUiKnowledgeCurated,
                pageModelBundle,
                canonicalInteractionModel,
                retrievalContext,
                assertionContracts,
                pageModelEnrichments,
                templateCapabilities,
                dbStableLocatorEvidence,
                List.of(),
                promptUiEvidence
        );
    }

    public AiContextPackage(
            String objective,
            NormalizedRequirementBundle normalizedRequirementBundle,
            GenerationPolicy generationPolicy,
            ProjectProfile projectProfile,
            TestPlan testPlan,
            CanonicalTestCaseBundle canonicalTestCaseBundle,
            UiTestPlan uiTestPlan,
            CanonicalPageFlowModel canonicalPageFlowModel,
            MappedUiKnowledge mappedUiKnowledge,
            MappedUiKnowledgeCurated mappedUiKnowledgeCurated,
            PageModelBundle pageModelBundle,
            CanonicalUiInteractionModel canonicalInteractionModel,
            UiKnowledgeRetrievalContext retrievalContext,
            List<AssertionContract> assertionContracts,
            List<PageModelEnrichmentRecord> pageModelEnrichments,
            List<String> templateCapabilities,
            PromptUiEvidence promptUiEvidence
    ) {
        this(
                objective,
                normalizedRequirementBundle,
                generationPolicy,
                projectProfile,
                testPlan,
                canonicalTestCaseBundle,
                uiTestPlan,
                canonicalPageFlowModel,
                mappedUiKnowledge,
                mappedUiKnowledgeCurated,
                pageModelBundle,
                canonicalInteractionModel,
                retrievalContext,
                assertionContracts,
                pageModelEnrichments,
                templateCapabilities,
                List.of(),
                List.of(),
                promptUiEvidence
        );
    }
}
