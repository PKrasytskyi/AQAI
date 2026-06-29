package ua.demo.agentlab.ui.discovery.mapping.model;

public record AssertionHint(
        String hintType,
        String target,
        String description,
        double confidenceScore
) {
    public AssertionHint {
        hintType = hintType == null ? "" : hintType.trim();
        target = target == null ? "" : target.trim();
        description = description == null ? "" : description.trim();
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
