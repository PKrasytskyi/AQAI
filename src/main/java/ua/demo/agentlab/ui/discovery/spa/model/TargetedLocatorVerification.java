package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

public record TargetedLocatorVerification(
        String pageId,
        String route,
        String pageFingerprintHash,
        String componentId,
        String locatorId,
        String elementId,
        String strategy,
        String value,
        double qualityScore,
        boolean verified,
        String reason,
        List<String> requirementIds
) {
    public TargetedLocatorVerification {
        pageId = safe(pageId);
        route = safe(route);
        pageFingerprintHash = safe(pageFingerprintHash);
        componentId = safe(componentId);
        locatorId = safe(locatorId);
        elementId = safe(elementId);
        strategy = safe(strategy);
        value = safe(value);
        qualityScore = Double.isFinite(qualityScore) ? Math.max(0.0d, Math.min(1.0d, qualityScore)) : 0.0d;
        reason = safe(reason);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
