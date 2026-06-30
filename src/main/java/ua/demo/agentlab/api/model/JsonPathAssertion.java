package ua.demo.agentlab.api.model;

public record JsonPathAssertion(
        ApiAssertionType type,
        String jsonPath,
        String expectedValue
) {
    public JsonPathAssertion {
        type = type == null ? ApiAssertionType.JSON_FIELD_EXISTS : type;
        jsonPath = safe(jsonPath);
        expectedValue = safe(expectedValue);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
