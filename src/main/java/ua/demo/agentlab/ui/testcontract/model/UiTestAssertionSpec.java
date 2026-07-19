package ua.demo.agentlab.ui.testcontract.model;

public record UiTestAssertionSpec(
        int order,
        String page,
        String method,
        UiTestAssertionMode mode,
        String expectedValue,
        String requirementId,
        String message
) {
    public UiTestAssertionSpec {
        order = Math.max(0, order);
        page = safe(page);
        method = safe(method);
        mode = mode == null ? UiTestAssertionMode.TRUE : mode;
        expectedValue = safe(expectedValue);
        requirementId = safe(requirementId);
        message = safe(message);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
