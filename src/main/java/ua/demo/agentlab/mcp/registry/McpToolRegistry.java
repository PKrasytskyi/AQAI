package ua.demo.agentlab.mcp.registry;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.result.McpToolResult;

import java.util.Set;

public interface McpToolRegistry {

    <I, O extends McpToolResult> void register(McpTool<I, O> tool);

    <I, O extends McpToolResult> O execute(McpToolName name, I input, Class<O> resultType);

    boolean supports(McpToolName name);

    Set<McpToolName> availableTools();
}
