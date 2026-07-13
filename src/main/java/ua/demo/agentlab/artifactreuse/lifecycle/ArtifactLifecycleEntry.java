package ua.demo.agentlab.artifactreuse.lifecycle;

import ua.demo.agentlab.artifactreuse.model.ArtifactStatus;

public record ArtifactLifecycleEntry(
        String pageName,
        String artifactId,
        String fingerprint,
        ArtifactStatus status,
        boolean reusable,
        double qualityScore,
        String reason,
        boolean registryAttempted,
        boolean registryUpdated
) {
    public ArtifactLifecycleEntry {
        pageName = safe(pageName);
        artifactId = safe(artifactId);
        fingerprint = safe(fingerprint);
        status = status == null ? ArtifactStatus.NEEDS_REVIEW : status;
        qualityScore = Math.max(0.0d, Math.min(100.0d, qualityScore));
        reason = safe(reason);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
