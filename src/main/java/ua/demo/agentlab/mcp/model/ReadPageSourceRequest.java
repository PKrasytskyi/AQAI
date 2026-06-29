package ua.demo.agentlab.mcp.model;

public record ReadPageSourceRequest(
        String path,
        int maxChars
) {
}
