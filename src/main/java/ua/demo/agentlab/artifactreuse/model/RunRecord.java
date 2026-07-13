package ua.demo.agentlab.artifactreuse.model;

public record RunRecord(
        String runId,
        String appId,
        String baseUrlHash,
        String requirementSetHash,
        String discoverySessionId,
        String schemaVersion,
        String createdAt,
        String sourceAgent
) {
    public RunRecord {
        runId = safe(runId);
        appId = safe(appId);
        baseUrlHash = safe(baseUrlHash);
        requirementSetHash = safe(requirementSetHash);
        discoverySessionId = safe(discoverySessionId);
        schemaVersion = safe(schemaVersion);
        createdAt = safe(createdAt);
        sourceAgent = safe(sourceAgent);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
