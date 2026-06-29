package ua.demo.agentlab.ai.rag.intelligence.model;

public record ControllerRouteDefinition(
        String className,
        String packageName,
        String relativePath,
        String methodName,
        String httpMethod,
        String basePath,
        String routePath,
        String fullPath
) {
}
