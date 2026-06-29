package ua.demo.agentlab.mcp.model;

import ua.demo.agentlab.mcp.result.McpExecutionStatus;
import ua.demo.agentlab.mcp.result.McpToolResult;

import java.nio.file.Path;

public record ReadFileResult(
        McpExecutionStatus status,
        String message,
        Path absolutePath,
        String content,
        boolean truncated
) implements McpToolResult {
}
