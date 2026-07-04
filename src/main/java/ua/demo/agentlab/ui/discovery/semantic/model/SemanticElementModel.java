package ua.demo.agentlab.ui.discovery.semantic.model;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.util.List;

public record SemanticElementModel(
        String elementId,
        String name,
        String elementType,
        String role,
        String visibleText,
        List<PageLocatorModel> locatorCandidates,
        List<ActionCandidate> actionCandidates,
        List<BusinessIntentCandidate> businessIntentCandidates,
        double confidence
) {
    public SemanticElementModel {
        elementId = safe(elementId);
        name = safe(name);
        elementType = safe(elementType).toUpperCase();
        role = safe(role);
        visibleText = safe(visibleText);
        locatorCandidates = locatorCandidates == null ? List.of() : List.copyOf(locatorCandidates);
        actionCandidates = actionCandidates == null ? List.of() : List.copyOf(actionCandidates);
        businessIntentCandidates = businessIntentCandidates == null ? List.of() : List.copyOf(businessIntentCandidates);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
