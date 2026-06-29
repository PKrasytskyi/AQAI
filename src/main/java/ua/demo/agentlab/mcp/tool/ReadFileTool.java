package ua.demo.agentlab.mcp.tool;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.model.ReadFileRequest;
import ua.demo.agentlab.mcp.model.ReadFileResult;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.nio.file.Path;

public class ReadFileTool extends AbstractWorkspaceTextReadTool implements McpTool<ReadFileRequest, ReadFileResult> {

    public ReadFileTool(Path workspaceRoot) {
        super(workspaceRoot);
    }

    @Override
    public McpToolName name() {
        return McpToolName.READ_FILE;
    }

    @Override
    public Class<ReadFileRequest> inputType() {
        return ReadFileRequest.class;
    }

    @Override
    public ReadFileResult execute(ReadFileRequest input) {
        if (input == null || input.path() == null || input.path().isBlank()) {
            return new ReadFileResult(McpExecutionStatus.INVALID_INPUT, "File path is required", null, null, false);
        }
        TextReadOutcome outcome = readText(input.path(), input.maxChars());
        if (!outcome.exists()) {
            return new ReadFileResult(
                    McpExecutionStatus.NOT_FOUND,
                    "File not found: " + input.path(),
                    outcome.absolutePath(),
                    null,
                    false
            );
        }
        return new ReadFileResult(
                McpExecutionStatus.SUCCESS,
                "File read successfully",
                outcome.absolutePath(),
                outcome.content(),
                outcome.truncated()
        );
    }
}
