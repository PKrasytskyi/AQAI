package ua.demo.agentlab.ai.rag.intelligence.model;

public record LayerComponentDefinition(
        String className,
        String packageName,
        String relativePath,
        LayerComponentType componentType
) {
}
