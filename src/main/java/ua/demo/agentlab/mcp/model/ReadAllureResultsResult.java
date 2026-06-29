package ua.demo.agentlab.mcp.model;

import ua.demo.agentlab.mcp.result.McpExecutionStatus;
import ua.demo.agentlab.mcp.result.McpToolResult;

import java.nio.file.Path;
import java.util.List;

public record ReadAllureResultsResult(
        McpExecutionStatus status,
        String message,
        Path absoluteDirectory,
        List<ArtifactTextSnippet> files
) implements McpToolResult {
}
