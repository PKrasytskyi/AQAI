package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;

import java.util.List;

/** Rendered target page captured immediately after a requirement-scoped live transition. */
public record LiveTargetPageSnapshot(
        String sourcePageId,
        String sourceRoute,
        String actionId,
        String targetPageId,
        String targetRoute,
        List<String> requirementIds,
        DiscoveredPageSnapshot snapshot
) {
    public LiveTargetPageSnapshot {
        sourcePageId = safe(sourcePageId);
        sourceRoute = safe(sourceRoute);
        actionId = safe(actionId);
        targetPageId = safe(targetPageId);
        targetRoute = safe(targetRoute);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
