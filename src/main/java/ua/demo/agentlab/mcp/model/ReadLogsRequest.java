package ua.demo.agentlab.mcp.model;

public record ReadLogsRequest(
        String path,
        int maxChars
) {
}
