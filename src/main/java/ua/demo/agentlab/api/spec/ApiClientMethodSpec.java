package ua.demo.agentlab.api.spec;

import ua.demo.agentlab.api.model.ApiAuthMode;
import ua.demo.agentlab.api.model.HttpMethod;

import java.util.List;

public record ApiClientMethodSpec(
        String methodName,
        HttpMethod httpMethod,
        String pathTemplate,
        String requestDtoClassName,
        String responseDtoClassName,
        ApiAuthMode authMode,
        boolean returnsModel,
        List<String> pathParameters
) {
    public ApiClientMethodSpec {
        methodName = safe(methodName);
        httpMethod = httpMethod == null ? HttpMethod.GET : httpMethod;
        pathTemplate = normalizePath(pathTemplate);
        requestDtoClassName = safe(requestDtoClassName);
        responseDtoClassName = safe(responseDtoClassName);
        authMode = authMode == null ? ApiAuthMode.NONE : authMode;
        pathParameters = pathParameters == null ? List.of() : List.copyOf(pathParameters);
    }

    public boolean hasRequestBody() {
        return !requestDtoClassName.isBlank();
    }

    private static String normalizePath(String value) {
        String normalized = safe(value);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
