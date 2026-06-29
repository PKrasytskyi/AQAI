package ua.demo.agentlab.mcp.agent;

import ua.demo.agentlab.mcp.registry.DefaultMcpToolRegistry;
import ua.demo.agentlab.mcp.registry.McpToolRegistry;
import ua.demo.agentlab.mcp.trace.McpExecutionTrace;
import ua.demo.agentlab.mcp.trace.TracingMcpToolRegistry;
import ua.demo.agentlab.mcp.tool.CreatePageObjectTool;
import ua.demo.agentlab.mcp.tool.ReadAllureResultsTool;
import ua.demo.agentlab.mcp.tool.ReadFileTool;
import ua.demo.agentlab.mcp.tool.ReadLogsTool;
import ua.demo.agentlab.mcp.tool.ReadPageSourceTool;
import ua.demo.agentlab.mcp.tool.ReadScreenshotTool;
import ua.demo.agentlab.mcp.tool.RunMavenTestTool;
import ua.demo.agentlab.mcp.tool.UpdateTestTool;

import java.nio.file.Path;

public class LocalMcpExecutionAgent implements McpCapableAgent {

    private final McpToolRegistry toolRegistry;
    private final McpExecutionTrace executionTrace;

    public LocalMcpExecutionAgent(Path workspaceRoot) {
        DefaultMcpToolRegistry registry = new DefaultMcpToolRegistry();
        registry.register(new ReadFileTool(workspaceRoot));
        registry.register(new CreatePageObjectTool(workspaceRoot));
        registry.register(new UpdateTestTool(workspaceRoot));
        registry.register(new RunMavenTestTool(workspaceRoot));
        registry.register(new ReadAllureResultsTool(workspaceRoot));
        registry.register(new ReadScreenshotTool(workspaceRoot));
        registry.register(new ReadPageSourceTool(workspaceRoot));
        registry.register(new ReadLogsTool(workspaceRoot));
        this.executionTrace = new McpExecutionTrace();
        this.toolRegistry = new TracingMcpToolRegistry(registry, executionTrace);
    }

    @Override
    public McpToolRegistry toolRegistry() {
        return toolRegistry;
    }

    public McpExecutionTrace executionTrace() {
        return executionTrace;
    }
}
