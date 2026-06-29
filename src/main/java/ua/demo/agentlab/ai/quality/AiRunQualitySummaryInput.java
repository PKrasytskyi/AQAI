package ua.demo.agentlab.ai.quality;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.Map;

public record AiRunQualitySummaryInput(
        KnowledgeRunMetadata knowledgeRunMetadata,
        NormalizedRequirementBundle normalizedRequirementBundle,
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        MappedUiKnowledge mappedUiKnowledge,
        Map<String, String> artifacts
) {
    public AiRunQualitySummaryInput {
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
    }
}
