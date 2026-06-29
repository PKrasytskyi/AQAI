package ua.demo.agentlab.ui.discovery.mapping.model;

public record MappedAction(
        String actionId,
        String actionName,
        String actionType,
        String sourceElementId,
        String targetPageId,
        String description,
        double confidenceScore
) {
    public MappedAction {
        actionId = actionId == null ? "" : actionId.trim();
        actionName = actionName == null ? "" : actionName.trim();
        actionType = actionType == null ? "" : actionType.trim();
        sourceElementId = sourceElementId == null ? "" : sourceElementId.trim();
        targetPageId = targetPageId == null ? "" : targetPageId.trim();
        description = description == null ? "" : description.trim();
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
