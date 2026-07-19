package ua.demo.agentlab.ui.contract;

import java.util.Objects;

public record AssertionIntent(
        AssertionIntentKind kind,
        String target,
        String expectedValue
) {

    public AssertionIntent(AssertionIntentKind kind, String expectedValue) {
        this(kind, "", expectedValue);
    }

    public AssertionIntent {
        Objects.requireNonNull(kind, "kind must not be null");
        target = blankToNull(target);
        expectedValue = blankToNull(expectedValue);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
