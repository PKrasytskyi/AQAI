package ua.demo.agentlab.ui.discovery.pagemodel.model;

public record PageApiRelationModel(
        String relationId,
        String elementId,
        String endpoint,
        String relationType,
        double confidenceScore,
        String reason
) {
    public PageApiRelationModel {
        relationId = relationId == null ? "" : relationId.trim();
        elementId = elementId == null ? "" : elementId.trim();
        endpoint = endpoint == null ? "" : endpoint.trim();
        relationType = relationType == null ? "" : relationType.trim();
        reason = reason == null ? "" : reason.trim();
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
