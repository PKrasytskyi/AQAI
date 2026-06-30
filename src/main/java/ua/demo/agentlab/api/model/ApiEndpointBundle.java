package ua.demo.agentlab.api.model;

import java.util.List;
import java.util.Optional;

public record ApiEndpointBundle(
        String source,
        List<ApiEndpointModel> endpoints
) {
    public ApiEndpointBundle {
        source = source == null ? "" : source.trim();
        endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
    }

    public Optional<ApiEndpointModel> find(String endpointId) {
        String normalized = endpointId == null ? "" : endpointId.trim();
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return endpoints.stream()
                .filter(endpoint -> normalized.equals(endpoint.endpointId()))
                .findFirst();
    }
}
