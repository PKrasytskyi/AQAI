package ua.demo.agentlab.ai.rag.intelligence.model;

public record EndpointMatch(
        ControllerRouteDefinition controllerRoute,
        OpenApiEndpointDefinition openApiEndpoint,
        boolean matched,
        double confidenceScore,
        String matchStrategy
) {
    public EndpointMatch {
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
        matchStrategy = matchStrategy == null ? "" : matchStrategy.trim();
    }
}
