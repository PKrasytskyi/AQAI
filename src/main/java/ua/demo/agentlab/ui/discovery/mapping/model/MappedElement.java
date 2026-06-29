package ua.demo.agentlab.ui.discovery.mapping.model;

import java.util.List;

public record MappedElement(
        String elementId,
        String semanticName,
        String elementType,
        String role,
        String text,
        boolean clickable,
        boolean visible,
        List<LocatorCandidate> locatorCandidates,
        List<String> supportedActions,
        double confidenceScore
) {
    public MappedElement {
        elementId = elementId == null ? "" : elementId.trim();
        semanticName = semanticName == null ? "" : semanticName.trim();
        elementType = elementType == null ? "" : elementType.trim();
        role = role == null ? "" : role.trim();
        text = text == null ? "" : text.trim();
        locatorCandidates = locatorCandidates == null ? List.of() : List.copyOf(locatorCandidates);
        supportedActions = supportedActions == null ? List.of() : List.copyOf(supportedActions);
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
