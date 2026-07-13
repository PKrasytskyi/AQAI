package ua.demo.agentlab.artifactreuse.model;

public record QualityGateRecord(
        String gateId,
        String gateType,
        String status,
        String summary,
        int issueCount,
        String createdAt
) {
    public QualityGateRecord {
        gateId = safe(gateId);
        gateType = safe(gateType);
        status = safe(status);
        summary = safe(summary);
        issueCount = Math.max(0, issueCount);
        createdAt = safe(createdAt);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
