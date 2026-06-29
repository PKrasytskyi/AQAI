package ua.demo.agentlab.mcp.tool;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.model.ReadLogsRequest;
import ua.demo.agentlab.mcp.model.ReadLogsResult;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.nio.file.Path;

public class ReadLogsTool extends AbstractWorkspaceTextReadTool implements McpTool<ReadLogsRequest, ReadLogsResult> {

    public ReadLogsTool(Path workspaceRoot) {
        super(workspaceRoot);
    }

    @Override
    public McpToolName name() {
        return McpToolName.READ_LOGS;
    }

    @Override
    public Class<ReadLogsRequest> inputType() {
        return ReadLogsRequest.class;
    }

    @Override
    public ReadLogsResult execute(ReadLogsRequest input) {
        if (input == null || input.path() == null || input.path().isBlank()) {
            return new ReadLogsResult(
                    McpExecutionStatus.INVALID_INPUT,
                    "Logs path is required",
                    null,
                    null,
                    false
            );
        }
        TextReadOutcome outcome = readText(input.path(), input.maxChars());
        if (!outcome.exists()) {
            return new ReadLogsResult(
                    McpExecutionStatus.NOT_FOUND,
                    "Logs file not found",
                    outcome.absolutePath(),
                    null,
                    false
            );
        }
        return new ReadLogsResult(
                McpExecutionStatus.SUCCESS,
                "Logs read successfully",
                outcome.absolutePath(),
                outcome.content(),
                outcome.truncated()
        );
    }
}
