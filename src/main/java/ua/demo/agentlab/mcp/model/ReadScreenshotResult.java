package ua.demo.agentlab.mcp.model;

import ua.demo.agentlab.mcp.result.McpExecutionStatus;
import ua.demo.agentlab.mcp.result.McpToolResult;

import java.nio.file.Path;

public record ReadScreenshotResult(
        McpExecutionStatus status,
        String message,
        Path absolutePath,
        String mediaType,
        long byteSize,
        String base64Content
) implements McpToolResult {
}
