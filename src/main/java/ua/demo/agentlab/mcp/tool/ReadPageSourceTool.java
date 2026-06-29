package ua.demo.agentlab.mcp.tool;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.model.ReadPageSourceRequest;
import ua.demo.agentlab.mcp.model.ReadPageSourceResult;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.nio.file.Path;

public class ReadPageSourceTool extends AbstractWorkspaceTextReadTool
        implements McpTool<ReadPageSourceRequest, ReadPageSourceResult> {

    public ReadPageSourceTool(Path workspaceRoot) {
        super(workspaceRoot);
    }

    @Override
    public McpToolName name() {
        return McpToolName.READ_PAGE_SOURCE;
    }

    @Override
    public Class<ReadPageSourceRequest> inputType() {
        return ReadPageSourceRequest.class;
    }

    @Override
    public ReadPageSourceResult execute(ReadPageSourceRequest input) {
        if (input == null || input.path() == null || input.path().isBlank()) {
            return new ReadPageSourceResult(
                    McpExecutionStatus.INVALID_INPUT,
                    "Page source path is required",
                    null,
                    null,
                    false
            );
        }
        TextReadOutcome outcome = readText(input.path(), input.maxChars());
        if (!outcome.exists()) {
            return new ReadPageSourceResult(
                    McpExecutionStatus.NOT_FOUND,
                    "Page source file not found",
                    outcome.absolutePath(),
                    null,
                    false
            );
        }
        return new ReadPageSourceResult(
                McpExecutionStatus.SUCCESS,
                "Page source read successfully",
                outcome.absolutePath(),
                outcome.content(),
                outcome.truncated()
        );
    }
}
