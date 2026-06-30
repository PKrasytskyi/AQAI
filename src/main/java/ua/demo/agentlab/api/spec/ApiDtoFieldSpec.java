package ua.demo.agentlab.api.spec;

public record ApiDtoFieldSpec(
        String name,
        String javaType,
        boolean required
) {
    public ApiDtoFieldSpec {
        name = safe(name);
        javaType = javaType == null || javaType.isBlank() ? "String" : javaType.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
