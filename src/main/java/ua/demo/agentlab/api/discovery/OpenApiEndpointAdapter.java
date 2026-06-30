package ua.demo.agentlab.api.discovery;

import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiEndpointDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiSpecification;
import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiEndpointEvidence;
import ua.demo.agentlab.api.model.ApiEndpointEvidenceSource;
import ua.demo.agentlab.api.model.ApiEndpointModel;
import ua.demo.agentlab.api.model.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OpenApiEndpointAdapter {

    public ApiEndpointBundle adapt(List<OpenApiSpecification> specifications) {
        List<ApiEndpointModel> endpoints = new ArrayList<>();
        if (specifications != null) {
            for (OpenApiSpecification specification : specifications) {
                if (specification == null) {
                    continue;
                }
                for (OpenApiEndpointDefinition endpoint : specification.endpoints()) {
                    endpoints.add(adaptOne(endpoint));
                }
            }
        }
        return new ApiEndpointBundle("openapi", endpoints);
    }

    private ApiEndpointModel adaptOne(OpenApiEndpointDefinition endpoint) {
        HttpMethod method = parseMethod(endpoint.httpMethod());
        String path = endpoint.path();
        String operationName = firstNonBlank(endpoint.operationId(), endpoint.summary());
        return new ApiEndpointModel(
                endpointId(method, path),
                method,
                path,
                operationName,
                operationName,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(new ApiEndpointEvidence(
                        ApiEndpointEvidenceSource.OPENAPI,
                        endpoint.sourcePath() + "#" + endpoint.httpMethod() + " " + endpoint.path(),
                        0.95d
                )),
                0.95d
        );
    }

    private HttpMethod parseMethod(String value) {
        try {
            return HttpMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return HttpMethod.GET;
        }
    }

    private String endpointId(HttpMethod method, String path) {
        String normalizedPath = path == null || path.isBlank() ? "" : path.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return method.name() + ":" + normalizedPath;
    }

    private String firstNonBlank(String first, String second) {
        String value = first == null ? "" : first.trim();
        return value.isBlank() ? second == null ? "" : second.trim() : value;
    }
}
