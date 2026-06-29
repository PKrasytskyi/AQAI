package ua.demo.agentlab.ui.discovery.pagemodel.model;

public record PageActionModel(
        String actionId,
        String actionType,
        String actionName,
        String description,
        double confidenceScore
) {
    public PageActionModel {
        actionId = actionId == null ? "" : actionId.trim();
        actionType = actionType == null ? "" : actionType.trim();
        actionName = actionName == null ? "" : actionName.trim();
        description = description == null ? "" : description.trim();
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
