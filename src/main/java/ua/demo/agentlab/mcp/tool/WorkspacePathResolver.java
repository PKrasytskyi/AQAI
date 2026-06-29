package ua.demo.agentlab.mcp.tool;

import java.nio.file.Path;

public class WorkspacePathResolver {

    private final Path workspaceRoot;

    public WorkspacePathResolver(Path workspaceRoot) {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("workspaceRoot cannot be null");
        }
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public Path resolveFile(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be blank");
        }
        Path candidate = Path.of(path.trim());
        Path resolved = candidate.isAbsolute() ? candidate.normalize() : workspaceRoot.resolve(candidate).normalize();
        if (!resolved.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("Path escapes workspace root: " + path);
        }
        return resolved;
    }
}
