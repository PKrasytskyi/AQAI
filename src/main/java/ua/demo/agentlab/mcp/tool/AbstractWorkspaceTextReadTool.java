package ua.demo.agentlab.mcp.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class AbstractWorkspaceTextReadTool {

    protected final WorkspacePathResolver pathResolver;

    protected AbstractWorkspaceTextReadTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    protected TextReadOutcome readText(String path, int maxChars) {
        try {
            Path absolutePath = pathResolver.resolveFile(path);
            if (!Files.exists(absolutePath)) {
                return new TextReadOutcome(absolutePath, null, false, false);
            }
            String content = Files.readString(absolutePath, StandardCharsets.UTF_8);
            boolean truncated = maxChars > 0 && content.length() > maxChars;
            String effectiveContent = truncated ? content.substring(0, maxChars) : content;
            return new TextReadOutcome(absolutePath, effectiveContent, truncated, true);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read text artifact: " + path, exception);
        }
    }

    protected record TextReadOutcome(
            Path absolutePath,
            String content,
            boolean truncated,
            boolean exists
    ) {
    }
}
