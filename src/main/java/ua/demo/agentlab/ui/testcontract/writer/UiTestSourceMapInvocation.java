package ua.demo.agentlab.ui.testcontract.writer;

import java.util.List;

public record UiTestSourceMapInvocation(
        String kind,
        String page,
        String method,
        List<String> sourceIds,
        int line
) {
    public UiTestSourceMapInvocation {
        kind = safe(kind);
        page = safe(page);
        method = safe(method);
        sourceIds = sourceIds == null ? List.of() : List.copyOf(sourceIds);
        line = Math.max(0, line);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
