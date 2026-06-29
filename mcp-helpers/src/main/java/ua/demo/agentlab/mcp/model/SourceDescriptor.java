package ua.demo.agentlab.mcp.model;

public record SourceDescriptor(
        SourceKind kind,
        String id,
        String title,
        String url
) {
}
