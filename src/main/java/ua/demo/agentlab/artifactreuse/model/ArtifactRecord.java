package ua.demo.agentlab.artifactreuse.model;

public record ArtifactRecord(
        String artifactId,
        ArtifactType artifactType,
        ArtifactTargetType targetType,
        String targetId,
        String fingerprint,
        String schemaVersion,
        String promptTemplateVersion,
        String model,
        double temperature,
        ArtifactStatus status,
        double qualityScore,
        boolean writerSucceeded,
        boolean compileSucceeded,
        String filePath,
        String createdAt,
        String lastUsedAt,
        long reuseCount
) {
    public ArtifactRecord {
        artifactId = safe(artifactId);
        artifactType = artifactType == null ? ArtifactType.UNKNOWN : artifactType;
        targetType = targetType == null ? ArtifactTargetType.UNKNOWN : targetType;
        targetId = safe(targetId);
        fingerprint = safe(fingerprint);
        schemaVersion = safe(schemaVersion);
        promptTemplateVersion = safe(promptTemplateVersion);
        model = safe(model);
        temperature = Math.max(0.0d, temperature);
        status = status == null ? ArtifactStatus.GENERATED : status;
        qualityScore = Math.max(0.0d, Math.min(100.0d, qualityScore));
        filePath = safe(filePath);
        createdAt = safe(createdAt);
        lastUsedAt = safe(lastUsedAt);
        reuseCount = Math.max(0L, reuseCount);
    }

    public boolean hasIdentity() {
        return !artifactId.isBlank() && !targetId.isBlank() && !fingerprint.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
