package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

/** Requirement-scoped source state and candidate evidence admitted to live browser verification. */
public record SourceStateBinding(
        String requirementId,
        String capability,
        String sourcePageId,
        String sourceRoute,
        String targetHint,
        List<String> componentIds,
        List<String> candidateLocatorIds,
        List<String> candidateActionIds,
        boolean liveVerificationEligible,
        List<String> reviewReasons
) {
    public SourceStateBinding {
        requirementId = safe(requirementId);
        capability = safe(capability);
        sourcePageId = safe(sourcePageId);
        sourceRoute = safe(sourceRoute);
        targetHint = safe(targetHint);
        componentIds = copy(componentIds);
        candidateLocatorIds = copy(candidateLocatorIds);
        candidateActionIds = copy(candidateActionIds);
        reviewReasons = copy(reviewReasons);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
