package ua.demo.agentlab.artifactreuse.registry;

import ua.demo.agentlab.artifactreuse.model.ArtifactType;

public record ArtifactLookupRequest(
        ArtifactType artifactType,
        String targetId,
        String fingerprint,
        String schemaVersion
) {
    public ArtifactLookupRequest {
        artifactType = artifactType == null ? ArtifactType.UNKNOWN : artifactType;
        targetId = safe(targetId);
        fingerprint = safe(fingerprint);
        schemaVersion = safe(schemaVersion);
    }

    public boolean complete() {
        return artifactType != ArtifactType.UNKNOWN && !targetId.isBlank() && !fingerprint.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
