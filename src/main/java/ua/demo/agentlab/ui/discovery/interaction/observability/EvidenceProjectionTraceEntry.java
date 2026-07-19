package ua.demo.agentlab.ui.discovery.interaction.observability;

import java.util.List;

public record EvidenceProjectionTraceEntry(
        String locatorEvidenceId,
        String semanticActionKey,
        String pageId,
        String componentId,
        List<String> requirementIds,
        boolean candidate,
        boolean requirementScoped,
        boolean topK,
        boolean liveVerified,
        String promotionStatus,
        boolean persisted,
        boolean catalogPrimary,
        boolean promptAllowed,
        String stoppedAt,
        String reason
) {
    public EvidenceProjectionTraceEntry {
        locatorEvidenceId = safe(locatorEvidenceId);
        semanticActionKey = safe(semanticActionKey);
        pageId = safe(pageId);
        componentId = safe(componentId);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        promotionStatus = safe(promotionStatus);
        stoppedAt = safe(stoppedAt);
        reason = safe(reason);
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
