package ua.demo.agentlab.ai.rag.intelligence.model;

public record OpenApiEndpointDefinition(
        String sourcePath,
        String httpMethod,
        String path,
        String operationId,
        String summary
) {
}
