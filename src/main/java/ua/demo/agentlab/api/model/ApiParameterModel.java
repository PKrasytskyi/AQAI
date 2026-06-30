package ua.demo.agentlab.api.model;

public record ApiParameterModel(
        String name,
        ApiParameterLocation location,
        String type,
        boolean required,
        String description
) {
    public ApiParameterModel {
        name = safe(name);
        location = location == null ? ApiParameterLocation.QUERY : location;
        type = safe(type);
        description = safe(description);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
