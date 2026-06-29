package ua.demo.agentlab.mcp.model;

import ua.demo.agentlab.mcp.result.McpExecutionStatus;
import ua.demo.agentlab.mcp.result.McpToolResult;

import java.nio.file.Path;

public record UpdateTestResult(
        McpExecutionStatus status,
        String message,
        Path absolutePath,
        boolean created
) implements McpToolResult {
}
