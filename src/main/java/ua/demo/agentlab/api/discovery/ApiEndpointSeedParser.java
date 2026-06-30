package ua.demo.agentlab.api.discovery;

import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiEndpointEvidence;
import ua.demo.agentlab.api.model.ApiEndpointEvidenceSource;
import ua.demo.agentlab.api.model.ApiEndpointModel;
import ua.demo.agentlab.api.model.ApiResponseModel;
import ua.demo.agentlab.api.model.HttpMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ApiEndpointSeedParser {

    public ApiEndpointBundle parse(String source, String endpointSeed) {
        if (endpointSeed == null || endpointSeed.isBlank()) {
            return new ApiEndpointBundle(source, List.of());
        }
        Map<String, ApiEndpointModel> endpoints = new LinkedHashMap<>();
        for (String token : endpointSeed.split("(?:[,;]|\\R)+")) {
            parseOne(source, token).forEach(endpoint -> endpoints.put(endpoint.endpointId(), endpoint));
        }
        return new ApiEndpointBundle(source, new ArrayList<>(endpoints.values()));
    }

    private List<ApiEndpointModel> parseOne(String source, String rawToken) {
        String token = rawToken == null ? "" : rawToken.trim();
        if (token.isBlank()) {
            return List.of();
        }
        String[] parts = token.split("\\s+", 3);
        if (parts.length < 2) {
            return List.of(endpoint(HttpMethod.GET, token, "", source));
        }
        HttpMethod method = parseMethod(parts[0]);
        String path = parts[1];
        String operationName = parts.length >= 3 ? parts[2] : "";
        return List.of(endpoint(method, path, operationName, source));
    }

    private ApiEndpointModel endpoint(HttpMethod method, String path, String operationName, String source) {
        return new ApiEndpointModel(
                endpointId(method, path),
                method,
                path,
                operationName,
                operationName,
                List.of(),
                null,
                List.of(new ApiResponseModel(defaultStatus(method), "", "", List.of())),
                List.of(),
                List.of(new ApiEndpointEvidence(ApiEndpointEvidenceSource.MANUAL, source + ":" + method + " " + path, 0.82d)),
                0.82d
        );
    }

    private HttpMethod parseMethod(String value) {
        try {
            return HttpMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return HttpMethod.GET;
        }
    }

    private int defaultStatus(HttpMethod method) {
        return method == HttpMethod.POST ? 201 : method == HttpMethod.DELETE ? 204 : 200;
    }

    private String endpointId(HttpMethod method, String path) {
        String normalizedPath = path == null || path.isBlank() ? "" : path.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return method.name() + ":" + normalizedPath;
    }
}
