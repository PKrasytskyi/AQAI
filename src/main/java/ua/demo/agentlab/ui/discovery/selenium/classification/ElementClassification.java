package ua.demo.agentlab.ui.discovery.selenium.classification;

import java.util.List;

public record ElementClassification(
        String elementId,
        String technicalType,
        String semanticType,
        List<String> actions,
        double confidenceScore
) {
    public ElementClassification {
        elementId = elementId == null ? "" : elementId.trim();
        technicalType = technicalType == null ? "" : technicalType.trim();
        semanticType = semanticType == null ? "" : semanticType.trim();
        actions = actions == null ? List.of() : List.copyOf(actions);
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
