package ua.demo.agentlab.ui.discovery.catalog;

public record RejectedLocatorCandidate(String locatorId, String reason) {
    public RejectedLocatorCandidate {
        locatorId = safe(locatorId);
        reason = safe(reason);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
