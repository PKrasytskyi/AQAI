package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;

import java.util.List;

public record SemanticComponentInventory(
        String componentId,
        String name,
        ComponentType type,
        String rootLocatorStrategy,
        String rootLocatorValue,
        String parentComponentId,
        List<String> elementIds,
        double confidence,
        List<CandidateLocatorEvidence> locators,
        List<CandidateActionEvidence> actions,
        List<String> risks,
        List<String> sourceTrace
) {
    public SemanticComponentInventory {
        componentId = safe(componentId);
        name = safe(name);
        type = type == null ? ComponentType.UNKNOWN : type;
        rootLocatorStrategy = safe(rootLocatorStrategy);
        rootLocatorValue = safe(rootLocatorValue);
        parentComponentId = safe(parentComponentId);
        elementIds = elementIds == null ? List.of() : List.copyOf(elementIds);
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        locators = locators == null ? List.of() : List.copyOf(locators);
        actions = actions == null ? List.of() : List.copyOf(actions);
        risks = risks == null ? List.of() : List.copyOf(risks);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
