package ua.demo.agentlab.ui.discovery.catalog;

import java.util.List;

public record LocatorElementCoverage(
        String pageId,
        String pageName,
        String route,
        String elementId,
        String componentId,
        String componentType,
        int candidateCount,
        int liveVerifiedCount,
        String primaryLocatorId,
        String standbyLocatorId,
        List<String> requirementIds,
        List<RejectedLocatorCandidate> rejected,
        String coverageGap
) {
    public LocatorElementCoverage {
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        elementId = safe(elementId);
        componentId = safe(componentId);
        componentType = safe(componentType);
        candidateCount = Math.max(0, candidateCount);
        liveVerifiedCount = Math.max(0, liveVerifiedCount);
        primaryLocatorId = safe(primaryLocatorId);
        standbyLocatorId = safe(standbyLocatorId);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        rejected = rejected == null ? List.of() : List.copyOf(rejected);
        coverageGap = safe(coverageGap);
    }

    public boolean hasCandidate() {
        return candidateCount > 0;
    }

    public boolean hasConfirmedPrimary() {
        return !primaryLocatorId.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
