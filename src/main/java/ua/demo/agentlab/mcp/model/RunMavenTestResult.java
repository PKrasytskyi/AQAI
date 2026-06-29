package ua.demo.agentlab.mcp.model;

import ua.demo.agentlab.mcp.result.McpExecutionStatus;
import ua.demo.agentlab.mcp.result.McpToolResult;

public record RunMavenTestResult(
        McpExecutionStatus status,
        String message,
        int exitCode,
        long durationMillis,
        String output
) implements McpToolResult {
}
