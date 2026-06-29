package ua.demo.agentlab.mcp.tool;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.model.ReadScreenshotRequest;
import ua.demo.agentlab.mcp.model.ReadScreenshotResult;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

public class ReadScreenshotTool implements McpTool<ReadScreenshotRequest, ReadScreenshotResult> {

    private final WorkspacePathResolver pathResolver;

    public ReadScreenshotTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    @Override
    public McpToolName name() {
        return McpToolName.READ_SCREENSHOT;
    }

    @Override
    public Class<ReadScreenshotRequest> inputType() {
        return ReadScreenshotRequest.class;
    }

    @Override
    public ReadScreenshotResult execute(ReadScreenshotRequest input) {
        if (input == null || input.path() == null || input.path().isBlank()) {
            return new ReadScreenshotResult(
                    McpExecutionStatus.INVALID_INPUT,
                    "Screenshot path is required",
                    null,
                    null,
                    0,
                    null
            );
        }

        try {
            Path absolutePath = pathResolver.resolveFile(input.path());
            if (!Files.exists(absolutePath)) {
                return new ReadScreenshotResult(
                        McpExecutionStatus.NOT_FOUND,
                        "Screenshot file not found",
                        absolutePath,
                        detectMediaType(absolutePath),
                        0,
                        null
                );
            }
            byte[] bytes = Files.readAllBytes(absolutePath);
            return new ReadScreenshotResult(
                    McpExecutionStatus.SUCCESS,
                    "Screenshot read successfully",
                    absolutePath,
                    detectMediaType(absolutePath),
                    bytes.length,
                    input.includeBase64() ? Base64.getEncoder().encodeToString(bytes) : null
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read screenshot: " + input.path(), exception);
        }
    }

    private String detectMediaType(Path absolutePath) {
        String fileName = absolutePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
