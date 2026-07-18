package ua.demo.agentlab.ai.context.slicing;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.context.*;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

import java.util.List;

/** Final assembly stage; it contains no ownership or route decisions. */
public final class ScopedContextAssembler {

    private final PromptUiEvidenceBuilder promptEvidenceBuilder;

    public ScopedContextAssembler(PromptUiEvidenceBuilder promptEvidenceBuilder) {
        this.promptEvidenceBuilder = promptEvidenceBuilder;
    }

    public AiContextPackage assemble(AiContextPackage source, ScopedContextParts parts) {
        AiContextPackage scoped = new AiContextPackage(source.objective(), parts.requirements(), source.generationPolicy(),
                source.projectProfile(), parts.testPlan(), parts.testCases(), parts.uiTestPlan(), parts.flows(),
                parts.mappedKnowledge(), parts.curatedKnowledge(), parts.pageModels(), parts.interactions(),
                parts.retrieval(), parts.assertions(), parts.enrichments(), source.templateCapabilities(),
                parts.dbLocators(), parts.catalogLocators(), PromptUiEvidence.empty("prompt-evidence:slicer-bootstrap"));
        return new AiContextPackage(scoped.objective(), scoped.normalizedRequirementBundle(), scoped.generationPolicy(),
                scoped.projectProfile(), scoped.testPlan(), scoped.canonicalTestCaseBundle(), scoped.uiTestPlan(),
                scoped.canonicalPageFlowModel(), scoped.mappedUiKnowledge(), scoped.mappedUiKnowledgeCurated(),
                scoped.pageModelBundle(), scoped.canonicalInteractionModel(), scoped.retrievalContext(),
                scoped.assertionContracts(), scoped.pageModelEnrichments(), scoped.templateCapabilities(),
                scoped.dbStableLocatorEvidence(), scoped.confirmedCatalogLocatorEvidence(),
                promptEvidenceBuilder.build(scoped));
    }

    public record ScopedContextParts(
            NormalizedRequirementBundle requirements,
            TestPlan testPlan,
            CanonicalTestCaseBundle testCases,
            UiTestPlan uiTestPlan,
            CanonicalPageFlowModel flows,
            MappedUiKnowledge mappedKnowledge,
            MappedUiKnowledgeCurated curatedKnowledge,
            PageModelBundle pageModels,
            CanonicalUiInteractionModel interactions,
            UiKnowledgeRetrievalContext retrieval,
            List<AssertionContract> assertions,
            List<PageModelEnrichmentRecord> enrichments,
            List<PromptLocatorEvidence> dbLocators,
            List<PromptLocatorEvidence> catalogLocators
    ) {
        public ScopedContextParts {
            assertions = assertions == null ? List.of() : List.copyOf(assertions);
            enrichments = enrichments == null ? List.of() : List.copyOf(enrichments);
            dbLocators = dbLocators == null ? List.of() : List.copyOf(dbLocators);
            catalogLocators = catalogLocators == null ? List.of() : List.copyOf(catalogLocators);
        }
    }
}
