package ua.demo.agentlab.mcp.registry;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.result.McpToolResult;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class DefaultMcpToolRegistry implements McpToolRegistry {

    private final Map<McpToolName, McpTool<?, ? extends McpToolResult>> tools = new EnumMap<>(McpToolName.class);

    @Override
    public <I, O extends McpToolResult> void register(McpTool<I, O> tool) {
        if (tool == null) {
            throw new IllegalArgumentException("tool cannot be null");
        }
        tools.put(tool.name(), tool);
    }

    @Override
    public <I, O extends McpToolResult> O execute(McpToolName name, I input, Class<O> resultType) {
        if (name == null || resultType == null) {
            throw new IllegalArgumentException("name and resultType cannot be null");
        }
        McpTool<?, ? extends McpToolResult> tool = tools.get(name);
        if (tool == null) {
            throw new IllegalStateException("No MCP tool registered for: " + name);
        }
        if (input != null && !tool.inputType().isInstance(input)) {
            throw new IllegalArgumentException(
                    "Input type mismatch for " + name + ". Expected " + tool.inputType().getName()
                            + " but got " + input.getClass().getName()
            );
        }
        McpTool<I, ? extends McpToolResult> typedTool = cast(tool);
        McpToolResult result = typedTool.execute(input);
        if (!resultType.isInstance(result)) {
            throw new IllegalStateException(
                    "Result type mismatch for " + name + ". Expected " + resultType.getName()
                            + " but got " + result.getClass().getName()
            );
        }
        return resultType.cast(result);
    }

    @Override
    public boolean supports(McpToolName name) {
        return tools.containsKey(name);
    }

    @Override
    public Set<McpToolName> availableTools() {
        return Set.copyOf(tools.keySet());
    }

    @SuppressWarnings("unchecked")
    private <I> McpTool<I, ? extends McpToolResult> cast(McpTool<?, ? extends McpToolResult> tool) {
        return (McpTool<I, ? extends McpToolResult>) tool;
    }
}
