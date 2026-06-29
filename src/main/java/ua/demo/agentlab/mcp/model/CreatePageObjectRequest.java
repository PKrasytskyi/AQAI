package ua.demo.agentlab.mcp.model;

public record CreatePageObjectRequest(
        String relativePath,
        String packageName,
        String className,
        String content
) {
}
