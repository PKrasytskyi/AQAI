package ua.demo.agentlab.ai.flow;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

public record FlowScopedKnowledgeInput(
        String objective,
        ProjectProfile projectProfile,
        TestPlan testPlan,
        NormalizedRequirementBundle normalizedRequirementBundle,
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        MappedUiKnowledge mappedUiKnowledge,
        KnowledgeRunMetadata knowledgeRunMetadata
) {
    public FlowScopedKnowledgeInput {
        objective = objective == null ? "" : objective.trim();
    }
}
