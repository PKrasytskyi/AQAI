package ua.demo.agentlab.api.model;

import java.util.List;

public record ApiResponseModel(
        int statusCode,
        String contentType,
        String schemaName,
        List<String> examples
) {
    public ApiResponseModel {
        contentType = safe(contentType);
        schemaName = safe(schemaName);
        examples = examples == null ? List.of() : List.copyOf(examples);
    }

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
