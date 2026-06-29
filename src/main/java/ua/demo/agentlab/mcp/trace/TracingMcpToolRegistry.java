package ua.demo.agentlab.mcp.trace;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.registry.McpToolRegistry;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;
import ua.demo.agentlab.mcp.result.McpToolResult;

import java.time.Instant;
import java.util.Set;

public class TracingMcpToolRegistry implements McpToolRegistry {

    private final McpToolRegistry delegate;
    private final McpExecutionTrace executionTrace;

    public TracingMcpToolRegistry(McpToolRegistry delegate, McpExecutionTrace executionTrace) {
        if (delegate == null || executionTrace == null) {
            throw new IllegalArgumentException("delegate and executionTrace cannot be null");
        }
        this.delegate = delegate;
        this.executionTrace = executionTrace;
    }

    @Override
    public <I, O extends McpToolResult> void register(McpTool<I, O> tool) {
        delegate.register(tool);
    }

    @Override
    public <I, O extends McpToolResult> O execute(McpToolName name, I input, Class<O> resultType) {
        Instant startedAt = Instant.now();
        try {
            O result = delegate.execute(name, input, resultType);
            Instant finishedAt = Instant.now();
            executionTrace.record(new McpExecutionTraceEntry(
                    name,
                    input == null ? "void" : input.getClass().getSimpleName(),
                    resultType.getSimpleName(),
                    result.status(),
                    result.message(),
                    startedAt,
                    finishedAt,
                    java.time.Duration.between(startedAt, finishedAt).toMillis()
            ));
            return result;
        } catch (RuntimeException exception) {
            Instant finishedAt = Instant.now();
            executionTrace.record(new McpExecutionTraceEntry(
                    name,
                    input == null ? "void" : input.getClass().getSimpleName(),
                    resultType == null ? "unknown" : resultType.getSimpleName(),
                    McpExecutionStatus.FAILED,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                    startedAt,
                    finishedAt,
                    java.time.Duration.between(startedAt, finishedAt).toMillis()
            ));
            throw exception;
        }
    }

    @Override
    public boolean supports(McpToolName name) {
        return delegate.supports(name);
    }

    @Override
    public Set<McpToolName> availableTools() {
        return delegate.availableTools();
    }
}
