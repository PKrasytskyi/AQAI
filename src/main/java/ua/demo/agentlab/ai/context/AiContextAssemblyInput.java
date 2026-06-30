package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

import java.util.List;

public record AiContextAssemblyInput(
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
        KnowledgeRunMetadata knowledgeRunMetadata,
        FlowScopedKnowledgePackage flowScopedKnowledgePackage,
        List<AssertionContract> assertionContracts,
        List<PageModelEnrichmentRecord> pageModelEnrichments
) {
    public AiContextAssemblyInput {
        objective = objective == null ? "" : objective.trim();
        assertionContracts = assertionContracts == null ? List.of() : List.copyOf(assertionContracts);
        pageModelEnrichments = pageModelEnrichments == null ? List.of() : List.copyOf(pageModelEnrichments);
    }

    public static AiContextAssemblyInput from(WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new AiContextAssemblyInput(
                state.getObjective(),
                state.getNormalizedRequirementBundle(),
                state.getGenerationPolicy(),
                state.getProjectProfile(),
                state.getTestPlan(),
                state.getCanonicalTestCaseBundle(),
                state.getUiTestPlan(),
                state.getCanonicalPageFlowModel(),
                state.getMappedUiKnowledge(),
                state.getMappedUiKnowledgeCurated(),
                state.getPageModelBundle(),
                state.getKnowledgeRunMetadata(),
                state.getFlowScopedKnowledgePackage(),
                state.getAssertionContracts(),
                state.getPageModelEnrichments()
        );
    }
}
