package ua.demo.agentlab.artifactreuse.model;

/** Audit record for one POM artifact lookup. It makes DB/local cache misses explainable in run artifacts. */
public record ArtifactReuseDecisionTrace(
        String pageName,
        String targetId,
        String fingerprint,
        boolean registryAttempted,
        boolean registryHit,
        String registryMessage,
        boolean stableFileHit,
        String stableFilePath,
        String decision,
        String decisionReason
) {
    public ArtifactReuseDecisionTrace {
        pageName = safe(pageName);
        targetId = safe(targetId);
        fingerprint = safe(fingerprint);
        registryMessage = safe(registryMessage);
        stableFilePath = safe(stableFilePath);
        decision = safe(decision);
        decisionReason = safe(decisionReason);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
