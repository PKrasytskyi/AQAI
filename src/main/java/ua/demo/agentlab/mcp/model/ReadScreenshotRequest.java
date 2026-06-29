package ua.demo.agentlab.mcp.model;

public record ReadScreenshotRequest(
        String path,
        boolean includeBase64
) {
}
