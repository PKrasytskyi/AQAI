package ua.demo.agentlab.ui.discovery.mapping.model;

public record MappedTransition(
        String transitionId,
        String fromPageId,
        String actionId,
        String toPageId,
        String toUrl,
        String actionType,
        boolean success,
        double confidenceScore
) {
    public MappedTransition {
        transitionId = transitionId == null ? "" : transitionId.trim();
        fromPageId = fromPageId == null ? "" : fromPageId.trim();
        actionId = actionId == null ? "" : actionId.trim();
        toPageId = toPageId == null ? "" : toPageId.trim();
        toUrl = toUrl == null ? "" : toUrl.trim();
        actionType = actionType == null ? "" : actionType.trim();
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
