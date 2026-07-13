package ua.demo.agentlab.artifactreuse.store;

import java.nio.file.Path;

public record StableArtifactWriteResult(
        boolean success,
        Path path,
        String message
) {
    public StableArtifactWriteResult {
        message = message == null ? "" : message.trim();
    }

    public static StableArtifactWriteResult success(Path path) {
        return new StableArtifactWriteResult(true, path, "stable artifact written");
    }

    public static StableArtifactWriteResult failed(String message) {
        return new StableArtifactWriteResult(false, null, message);
    }
}
