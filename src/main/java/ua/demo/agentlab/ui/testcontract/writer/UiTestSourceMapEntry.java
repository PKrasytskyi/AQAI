package ua.demo.agentlab.ui.testcontract.writer;

import java.util.List;

public record UiTestSourceMapEntry(
        String scenarioId,
        List<String> requirementIds,
        String className,
        String relativePath,
        int testMethodLine,
        List<UiTestSourceMapInvocation> invocations
) {
    public UiTestSourceMapEntry {
        scenarioId = safe(scenarioId);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        className = safe(className);
        relativePath = safe(relativePath);
        testMethodLine = Math.max(0, testMethodLine);
        invocations = invocations == null ? List.of() : List.copyOf(invocations);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
