package ua.demo.agentlab.api.model;

import java.util.List;

public record ApiEndpointModel(
        String endpointId,
        HttpMethod method,
        String path,
        String operationName,
        String businessCapability,
        List<ApiParameterModel> parameters,
        ApiRequestBodyModel requestBody,
        List<ApiResponseModel> responses,
        List<String> authRequirements,
        List<ApiEndpointEvidence> evidence,
        double confidence
) {
    public ApiEndpointModel {
        endpointId = firstNonBlank(endpointId, defaultEndpointId(method, path));
        method = method == null ? HttpMethod.GET : method;
        path = normalizePath(path);
        operationName = safe(operationName);
        businessCapability = safe(businessCapability);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        requestBody = requestBody == null ? new ApiRequestBodyModel("", "", false) : requestBody;
        responses = responses == null ? List.of() : List.copyOf(responses);
        authRequirements = authRequirements == null ? List.of() : List.copyOf(authRequirements);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    public boolean confirmed() {
        return confidence >= 0.70d || evidence.stream().anyMatch(ApiEndpointEvidence::confirmed);
    }

    public boolean hasResponseStatus(int statusCode) {
        return responses.stream().anyMatch(response -> response.statusCode() == statusCode);
    }

    private static String defaultEndpointId(HttpMethod method, String path) {
        String normalizedMethod = method == null ? "GET" : method.name();
        return normalizedMethod.toLowerCase() + ":" + normalizePath(path);
    }

    private static String normalizePath(String value) {
        String normalized = safe(value);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String firstNonBlank(String first, String second) {
        String value = safe(first);
        return value.isBlank() ? safe(second) : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
