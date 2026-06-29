package ua.demo.agentlab.ai.context;

import java.util.List;

public record AiContextScope(
        String stage,
        String scopeId,
        List<String> targetPageNames,
        List<String> targetRoutes,
        List<String> targetScenarioIds,
        List<String> targetRequirementIds
) {
    public AiContextScope {
        stage = normalize(stage);
        scopeId = normalize(scopeId);
        targetPageNames = targetPageNames == null ? List.of() : targetPageNames.stream()
                .map(AiContextScope::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        targetRoutes = targetRoutes == null ? List.of() : targetRoutes.stream()
                .map(AiContextScope::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        targetScenarioIds = targetScenarioIds == null ? List.of() : targetScenarioIds.stream()
                .map(AiContextScope::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        targetRequirementIds = targetRequirementIds == null ? List.of() : targetRequirementIds.stream()
                .map(AiContextScope::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
