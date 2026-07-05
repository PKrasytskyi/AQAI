package ua.demo.agentlab.ai.pageenrichment.model;

import java.util.List;
import java.util.Map;

public record PageModelEnrichmentInput(
        String pageId,
        String pageName,
        String route,
        String title,
        String featureGuess,
        List<String> actions,
        List<String> stableLocators,
        List<String> formFacts,
        List<String> assertionFacts,
        List<String> requirementActions,
        List<String> requirementAssertions,
        Map<String, List<String>> actionsByRequirement,
        Map<String, List<String>> postconditionsByRequirement,
        List<String> testCaseIds,
        List<String> requirementRefs,
        List<String> preconditions,
        String capability,
        List<String> semanticComponents,
        List<String> runtimeEvidence,
        List<String> knownGaps
) {
    public PageModelEnrichmentInput {
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        title = safe(title);
        featureGuess = safe(featureGuess);
        actions = copy(actions);
        stableLocators = copy(stableLocators);
        formFacts = copy(formFacts);
        assertionFacts = copy(assertionFacts);
        requirementActions = copy(requirementActions);
        requirementAssertions = copy(requirementAssertions);
        actionsByRequirement = copyFacts(actionsByRequirement);
        postconditionsByRequirement = copyFacts(postconditionsByRequirement);
        testCaseIds = copy(testCaseIds);
        requirementRefs = copy(requirementRefs);
        preconditions = copy(preconditions);
        capability = safe(capability);
        semanticComponents = copy(semanticComponents);
        runtimeEvidence = copy(runtimeEvidence);
        knownGaps = copy(knownGaps);
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
