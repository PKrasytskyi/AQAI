package ua.demo.agentlab.api.spec;

import java.util.List;

public record ApiClientSpec(
        String packageName,
        String className,
        String endpointConstantName,
        String endpointPath,
        List<ApiClientMethodSpec> methods
) {
    public ApiClientSpec {
        packageName = packageName == null || packageName.isBlank()
                ? "ua.demo.agentlab.api.generated.clients"
                : packageName.trim();
        className = className == null ? "" : className.trim();
        endpointConstantName = endpointConstantName == null || endpointConstantName.isBlank()
                ? "ENDPOINT"
                : endpointConstantName.trim();
        endpointPath = normalizePath(endpointPath);
        methods = methods == null ? List.of() : List.copyOf(methods);
    }

    private static String normalizePath(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }
}
