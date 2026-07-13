package ua.demo.agentlab.artifactreuse.registry;

import ua.demo.agentlab.artifactreuse.model.ArtifactRecord;

public record ArtifactLookupResult(
        boolean attempted,
        boolean hit,
        String backend,
        ArtifactRecord artifact,
        String message
) {
    public ArtifactLookupResult {
        backend = backend == null ? "" : backend.trim();
        message = message == null ? "" : message.trim();
    }

    public static ArtifactLookupResult skipped(String backend, String message) {
        return new ArtifactLookupResult(false, false, backend, null, message);
    }

    public static ArtifactLookupResult miss(String backend, String message) {
        return new ArtifactLookupResult(true, false, backend, null, message);
    }

    public static ArtifactLookupResult hit(String backend, ArtifactRecord artifact) {
        return new ArtifactLookupResult(true, true, backend, artifact, "stable artifact found");
    }
}
