package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

public record UiKnowledgeRetrievalRequest(
        ProjectProfile projectProfile,
        NormalizedRequirementBundle normalizedRequirementBundle,
        TestPlan testPlan,
        UiTestPlan uiTestPlan,
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        MappedUiKnowledge mappedUiKnowledge,
        KnowledgeRunMetadata runMetadata,
        CanonicalUiInteractionModel canonicalModel,
        List<String> preferredPageIds,
        List<String> preferredTerms,
        String querySeed
) {
    public UiKnowledgeRetrievalRequest {
        preferredPageIds = preferredPageIds == null ? List.of() : List.copyOf(preferredPageIds);
        preferredTerms = preferredTerms == null ? List.of() : List.copyOf(preferredTerms);
        querySeed = querySeed == null ? "" : querySeed.trim();
    }

    public static UiKnowledgeRetrievalRequest from(
            AiContextAssemblyInput input,
            CanonicalUiInteractionModel canonicalModel
    ) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        return new UiKnowledgeRetrievalRequest(
                input.projectProfile(),
                input.normalizedRequirementBundle(),
                input.testPlan(),
                input.uiTestPlan(),
                input.canonicalTestCaseBundle(),
                input.mappedUiKnowledge(),
                input.knowledgeRunMetadata(),
                canonicalModel,
                List.of(),
                List.of(),
                ""
        );
    }
}
