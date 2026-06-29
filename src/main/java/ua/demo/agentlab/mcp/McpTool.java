package ua.demo.agentlab.mcp;

import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.result.McpToolResult;

public interface McpTool<I, O extends McpToolResult> {

    McpToolName name();

    Class<I> inputType();

    O execute(I input);
}
