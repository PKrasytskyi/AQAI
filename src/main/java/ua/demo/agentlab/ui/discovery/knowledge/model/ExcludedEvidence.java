package ua.demo.agentlab.ui.discovery.knowledge.model;

public record ExcludedEvidence(
        String source,
        String evidenceType,
        String pageId,
        String reason,
        String value
) {
    public ExcludedEvidence {
        source = safe(source);
        evidenceType = safe(evidenceType);
        pageId = safe(pageId);
        reason = safe(reason);
        value = safe(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
