package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record KnowledgeEnrichmentRecord(
        String id,
        String enrichmentType,
        String relativePath,
        String className,
        String methodName,
        String codeSummary,
        String businessIntent,
        String businessMeaning,
        List<String> tags,
        List<String> dependencies,
        List<String> risks,
        List<String> stableLocators,
        double locatorStabilityScore,
        List<String> preconditions,
        List<String> postconditions,
        List<String> testCoverageGaps,
        List<String> failureClassifications,
        List<String> requirementTraceability,
        String enrichmentSource
) {
    public KnowledgeEnrichmentRecord {
        id = safe(id);
        enrichmentType = safe(enrichmentType);
        relativePath = safe(relativePath);
        className = safe(className);
        methodName = safe(methodName);
        codeSummary = safe(codeSummary);
        businessIntent = safe(businessIntent);
        businessMeaning = safe(businessMeaning);
        tags = tags == null ? List.of() : List.copyOf(tags);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        risks = risks == null ? List.of() : List.copyOf(risks);
        stableLocators = stableLocators == null ? List.of() : List.copyOf(stableLocators);
        locatorStabilityScore = Math.max(0.0d, Math.min(1.0d, locatorStabilityScore));
        preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
        postconditions = postconditions == null ? List.of() : List.copyOf(postconditions);
        testCoverageGaps = testCoverageGaps == null ? List.of() : List.copyOf(testCoverageGaps);
        failureClassifications = failureClassifications == null ? List.of() : List.copyOf(failureClassifications);
        requirementTraceability = requirementTraceability == null ? List.of() : List.copyOf(requirementTraceability);
        enrichmentSource = safe(enrichmentSource);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
