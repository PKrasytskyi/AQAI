package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

/** A requirement target accepted only from a browser-observed source-to-target transition. */
public record TargetStateBinding(
        String requirementId,
        String capability,
        String sourcePageId,
        String sourceRoute,
        String targetStateId,
        String targetRoute,
        String actionId,
        String locatorId,
        boolean transitionConfirmed,
        List<String> reviewReasons
) {
    public TargetStateBinding {
        requirementId = safe(requirementId);
        capability = safe(capability);
        sourcePageId = safe(sourcePageId);
        sourceRoute = safe(sourceRoute);
        targetStateId = safe(targetStateId);
        targetRoute = safe(targetRoute);
        actionId = safe(actionId);
        locatorId = safe(locatorId);
        reviewReasons = reviewReasons == null ? List.of() : List.copyOf(reviewReasons);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
