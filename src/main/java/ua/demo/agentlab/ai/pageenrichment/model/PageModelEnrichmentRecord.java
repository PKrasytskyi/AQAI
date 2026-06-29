package ua.demo.agentlab.ai.pageenrichment.model;

import java.util.List;
import java.util.Map;

public record PageModelEnrichmentRecord(
        String pageId,
        String pageName,
        String route,
        String businessIntent,
        String pageSummary,
        List<String> supportedActions,
        List<String> stableLocators,
        List<String> preconditions,
        List<String> postconditions,
        List<String> risks,
        List<String> coverageGaps,
        List<String> requirementTraceability,
        Map<String, List<String>> actionsByRequirement,
        Map<String, List<String>> postconditionsByRequirement,
        double confidenceScore,
        String enrichmentSource
) {
    public PageModelEnrichmentRecord {
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        businessIntent = safe(businessIntent);
        pageSummary = safe(pageSummary);
        supportedActions = copy(supportedActions);
        stableLocators = copyStableLocators(stableLocators);
        preconditions = copy(preconditions);
        postconditions = copy(postconditions);
        risks = copy(risks);
        coverageGaps = copy(coverageGaps);
        requirementTraceability = copy(requirementTraceability);
        actionsByRequirement = copyFacts(actionsByRequirement);
        postconditionsByRequirement = copyFacts(postconditionsByRequirement);
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
        enrichmentSource = enrichmentSource == null || enrichmentSource.isBlank()
                ? "rule-based"
                : enrichmentSource.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static List<String> copyStableLocators(List<String> values) {
        return copy(values).stream()
                .filter(value -> !isExternalLocator(value))
                .toList();
    }

    private static boolean isExternalLocator(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return normalized.contains("sameorigin=false")
                || normalized.contains("external-origin")
                || normalized.contains("external-link-text-xpath")
                || normalized.contains("http://")
                || normalized.contains("https://");
    }

    private static Map<String, List<String>> copyFacts(Map<String, List<String>> facts) {
        if (facts == null || facts.isEmpty()) {
            return Map.of();
        }
        return facts.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().trim(),
                        entry -> copy(entry.getValue()),
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ));
    }
}
