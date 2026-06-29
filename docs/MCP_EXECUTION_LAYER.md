# MCP Execution Layer

## Purpose

The MCP layer is the execution layer of the orchestrator.

It does not store knowledge and it does not decide strategy.

Its role is to give the agent controlled project actions:

- read a file
- create a page object
- update a test
- run `mvn test`
- read Allure results
- read screenshots
- read page source
- read logs

## Architectural Position

- `RAG` stores and retrieves project context
- `Planner / Agent` decides what should happen next
- `MCP` performs the action inside the workspace

In short:

- `RAG` = context
- `Planner` = decisions
- `MCP` = hands

## Package Layout

- `ua.demo.agentlab.mcp`
- `ua.demo.agentlab.mcp.model`
- `ua.demo.agentlab.mcp.result`
- `ua.demo.agentlab.mcp.tool`
- `ua.demo.agentlab.mcp.registry`
- `ua.demo.agentlab.mcp.agent`

## Core Contracts

- `McpTool<I, O>`
  Base contract for one execution tool.

- `McpToolName`
  Stable tool identifiers.

- `McpToolResult`
  Common result contract.

- `McpExecutionStatus`
  Execution outcome status.

- `McpToolRegistry`
  Runtime registry for tool lookup and execution.

- `LocalMcpExecutionAgent`
  Default local agent with all registered MCP tools.

- `McpExecutionTrace`
  In-memory execution journal for MCP tool calls.

- `McpExecutionTraceEntry`
  One execution event with timing and status.

- `TracingMcpToolRegistry`
  Wrapper registry that records every MCP tool execution.

## Implemented Tools

- `ReadFileTool`
- `CreatePageObjectTool`
- `UpdateTestTool`
- `RunMavenTestTool`
- `ReadAllureResultsTool`
- `ReadScreenshotTool`
- `ReadPageSourceTool`
- `ReadLogsTool`

## Execution Trace

`LocalMcpExecutionAgent` now wraps the registry with `TracingMcpToolRegistry`.

Each MCP execution records:

- tool name
- input type
- result type
- status
- message
- start time
- finish time
- duration in milliseconds

This does not change the MCP boundary.
It only adds observability.

## Safety

All file-based MCP tools resolve paths through `WorkspacePathResolver`.

This prevents path escape outside the workspace root and keeps MCP execution bounded to the project.

## Intended Flow

1. Planner or generation agent decides what action is needed
2. It prepares final code or a concrete read request
3. MCP tool executes that action
4. Result is returned to the agent for the next decision

## Important Boundary

MCP tools should not invent logic or decide test strategy.

For example:

- `CreatePageObjectTool` writes a prepared page object
- `UpdateTestTool` writes a prepared test
- `RunMavenTestTool` executes Maven

The planning and generation must stay outside MCP.
