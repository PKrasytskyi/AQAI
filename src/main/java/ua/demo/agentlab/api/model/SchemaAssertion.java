package ua.demo.agentlab.api.model;

public record SchemaAssertion(
        String schemaName,
        boolean required
) {
    public SchemaAssertion {
        schemaName = safe(schemaName);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
