package ua.demo.agentlab.testcase.generator;

import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

public record RequirementToTestCaseInput(
        ProjectProfile projectProfile,
        NormalizedRequirementBundle normalizedRequirementBundle,
        MappedUiKnowledge mappedUiKnowledge,
        FlowScopedKnowledgePackage flowScopedKnowledgePackage
) {
}
