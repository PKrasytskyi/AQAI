package ua.demo.agentlab.ui.contract;

import java.util.Objects;

public record UiOperationIntent(
        UiOperationKind kind,
        String target,
        String dataKey
) {

    public UiOperationIntent {
        Objects.requireNonNull(kind, "kind must not be null");
        target = blankToNull(target);
        dataKey = blankToNull(dataKey);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
