package ua.demo.agentlab.api.model;

public record HeaderAssertion(
        String name,
        String expectedValue
) {
    public HeaderAssertion {
        name = safe(name);
        expectedValue = safe(expectedValue);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
