package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

public record TargetedActionVerification(
        String pageId,
        String route,
        String pageFingerprintHash,
        String componentId,
        String actionId,
        String intent,
        String targetElementId,
        double confidence,
        boolean verified,
        String reason,
        List<String> requirementIds
) {
    public TargetedActionVerification {
        pageId = safe(pageId);
        route = safe(route);
        pageFingerprintHash = safe(pageFingerprintHash);
        componentId = safe(componentId);
        actionId = safe(actionId);
        intent = safe(intent);
        targetElementId = safe(targetElementId);
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        reason = safe(reason);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
