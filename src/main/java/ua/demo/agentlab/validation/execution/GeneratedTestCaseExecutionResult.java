package ua.demo.agentlab.validation.execution;

import java.util.List;

public record GeneratedTestCaseExecutionResult(
        String scenarioId,
        List<String> requirementIds,
        String testClass,
        String sourcePath,
        String status,
        long durationMillis,
        String failure,
        List<String> actionAndAssertionIds,
        List<String> evidencePaths
) {
    public GeneratedTestCaseExecutionResult {
        scenarioId = safe(scenarioId);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        testClass = safe(testClass);
        sourcePath = safe(sourcePath);
        status = safe(status);
        durationMillis = Math.max(0L, durationMillis);
        failure = safe(failure);
        actionAndAssertionIds = actionAndAssertionIds == null ? List.of() : List.copyOf(actionAndAssertionIds);
        evidencePaths = evidencePaths == null ? List.of() : List.copyOf(evidencePaths);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
