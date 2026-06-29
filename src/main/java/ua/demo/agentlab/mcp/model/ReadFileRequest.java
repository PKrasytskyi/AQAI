package ua.demo.agentlab.mcp.model;

public record ReadFileRequest(
        String path,
        int maxChars
) {
}
