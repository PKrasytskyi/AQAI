package ua.demo.agentlab.mcp.tool;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.model.UpdateTestRequest;
import ua.demo.agentlab.mcp.model.UpdateTestResult;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class UpdateTestTool implements McpTool<UpdateTestRequest, UpdateTestResult> {

    private final WorkspacePathResolver pathResolver;

    public UpdateTestTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    @Override
    public McpToolName name() {
        return McpToolName.UPDATE_TEST;
    }

    @Override
    public Class<UpdateTestRequest> inputType() {
        return UpdateTestRequest.class;
    }

    @Override
    public UpdateTestResult execute(UpdateTestRequest input) {
        if (input == null || input.relativePath() == null || input.relativePath().isBlank()
                || input.content() == null || input.content().isBlank()) {
            return new UpdateTestResult(
                    McpExecutionStatus.INVALID_INPUT,
                    "relativePath and content are required for test update",
                    null,
                    false
            );
        }

        try {
            Path absolutePath = pathResolver.resolveFile(input.relativePath());
            boolean created = !Files.exists(absolutePath);
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(absolutePath, input.content(), StandardCharsets.UTF_8);
            return new UpdateTestResult(
                    McpExecutionStatus.SUCCESS,
                    created ? "Test file created" : "Test file updated",
                    absolutePath,
                    created
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to update test file: " + input.relativePath(), exception);
        }
    }
}
