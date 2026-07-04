package ua.demo.agentlab.ai.ui.contract;

import java.util.List;

public record PomAssertionSpec(
        String methodName,
        String returnType,
        List<PomCheckSpec> checks,
        String combine
) {
    public PomAssertionSpec {
        methodName = safe(methodName);
        returnType = returnType == null || returnType.isBlank() ? "boolean" : returnType.trim();
        checks = checks == null ? List.of() : List.copyOf(checks);
        combine = combine == null || combine.isBlank() ? "AND" : combine.trim().toUpperCase();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
