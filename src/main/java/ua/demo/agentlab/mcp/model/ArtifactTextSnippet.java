package ua.demo.agentlab.mcp.model;

public record ArtifactTextSnippet(
        String relativePath,
        String contentSnippet,
        boolean truncated
) {
}
