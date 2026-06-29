package ua.demo.agentlab.ai.rag.intelligence.model;

public record DtoModelDefinition(
        String className,
        String packageName,
        String relativePath,
        DtoModelKind kind
) {
}
