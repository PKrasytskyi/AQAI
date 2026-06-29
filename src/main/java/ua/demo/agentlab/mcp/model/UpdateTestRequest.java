package ua.demo.agentlab.mcp.model;

public record UpdateTestRequest(
        String relativePath,
        String content
) {
}
