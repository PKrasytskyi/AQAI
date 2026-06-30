package ua.demo.agentlab.api.model;

public record ApiRequestBodyModel(
        String contentType,
        String schemaName,
        boolean required
) {
    public ApiRequestBodyModel {
        contentType = safe(contentType);
        schemaName = safe(schemaName);
    }

    public boolean present() {
        return !contentType.isBlank() || !schemaName.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
