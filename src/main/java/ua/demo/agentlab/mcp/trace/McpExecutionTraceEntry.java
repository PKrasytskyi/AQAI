package ua.demo.agentlab.mcp.trace;

import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.time.Instant;

public record McpExecutionTraceEntry(
        McpToolName toolName,
        String inputType,
        String resultType,
        McpExecutionStatus status,
        String message,
        Instant startedAt,
        Instant finishedAt,
        long durationMillis
) {
    public McpExecutionTraceEntry {
        if (toolName == null) {
            throw new IllegalArgumentException("toolName cannot be null");
        }
        inputType = inputType == null ? "" : inputType.trim();
        resultType = resultType == null ? "" : resultType.trim();
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        message = message == null ? "" : message.trim();
        if (startedAt == null || finishedAt == null) {
            throw new IllegalArgumentException("startedAt and finishedAt cannot be null");
        }
        durationMillis = Math.max(0L, durationMillis);
    }
}
