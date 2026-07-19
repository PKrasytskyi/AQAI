package ua.demo.agentlab.ui.discovery.catalog;

import java.util.List;

public record ConfirmedCatalogAction(
        String actionId,
        String intent,
        String targetElementId,
        String primaryLocatorId,
        String standbyLocatorId,
        double confidence,
        List<String> preconditions,
        List<String> postconditions,
        List<String> requirementIds
) {
    public ConfirmedCatalogAction {
        actionId = safe(actionId);
        intent = safe(intent);
        targetElementId = safe(targetElementId);
        primaryLocatorId = safe(primaryLocatorId);
        standbyLocatorId = safe(standbyLocatorId);
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
        postconditions = postconditions == null ? List.of() : List.copyOf(postconditions);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
