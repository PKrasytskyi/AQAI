package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record OpenApiSpecification(
        String sourcePath,
        String title,
        List<OpenApiEndpointDefinition> endpoints
) {
    public OpenApiSpecification {
        title = title == null ? "" : title;
        endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
    }
}
