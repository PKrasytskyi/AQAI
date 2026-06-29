package ua.demo.agentlab.mcp.tool;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.CreatePageObjectRequest;
import ua.demo.agentlab.mcp.model.CreatePageObjectResult;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreatePageObjectTool implements McpTool<CreatePageObjectRequest, CreatePageObjectResult> {

    private final WorkspacePathResolver pathResolver;

    public CreatePageObjectTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    @Override
    public McpToolName name() {
        return McpToolName.CREATE_PAGE_OBJECT;
    }

    @Override
    public Class<CreatePageObjectRequest> inputType() {
        return CreatePageObjectRequest.class;
    }

    @Override
    public CreatePageObjectResult execute(CreatePageObjectRequest input) {
        if (input == null || isBlank(input.relativePath()) || isBlank(input.content())) {
            return new CreatePageObjectResult(
                    McpExecutionStatus.INVALID_INPUT,
                    "relativePath and content are required for page object creation",
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
            return new CreatePageObjectResult(
                    McpExecutionStatus.SUCCESS,
                    created ? "Page object created" : "Page object updated",
                    absolutePath,
                    created
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create page object: " + input.relativePath(), exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
