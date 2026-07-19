package ua.demo.agentlab.ui.testcontract.model;

import java.util.List;

public record UiTestActionSpec(
        int order,
        String page,
        String method,
        List<UiTestArgumentSpec> arguments,
        List<String> sourceOperations
) {
    public UiTestActionSpec {
        order = Math.max(0, order);
        page = safe(page);
        method = safe(method);
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        sourceOperations = sourceOperations == null ? List.of() : List.copyOf(sourceOperations);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
