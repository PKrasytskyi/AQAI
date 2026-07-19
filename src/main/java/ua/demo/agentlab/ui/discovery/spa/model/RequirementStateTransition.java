package ua.demo.agentlab.ui.discovery.spa.model;

/** Requirement-owned result of executing one source-state action in a live browser. */
public record RequirementStateTransition(
        String requirementId,
        String sourcePageId,
        String sourceRoute,
        String actionId,
        String locatorId,
        String targetStateId,
        String targetRoute,
        boolean confirmed,
        String reason
) {
    public RequirementStateTransition {
        requirementId = safe(requirementId);
        sourcePageId = safe(sourcePageId);
        sourceRoute = safe(sourceRoute);
        actionId = safe(actionId);
        locatorId = safe(locatorId);
        targetStateId = safe(targetStateId);
        targetRoute = safe(targetRoute);
        reason = safe(reason);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
