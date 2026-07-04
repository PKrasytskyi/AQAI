package ua.demo.agentlab.ui.discovery.component.model;

import java.util.List;

public record SemanticComponentModel(
        String pageId,
        String componentId,
        String name,
        ComponentType type,
        String rootLocatorStrategy,
        String rootLocatorValue,
        List<String> elementIds,
        List<ScopedLocatorCandidate> locators,
        boolean reusable,
        double confidence,
        List<String> risks,
        List<String> sourceTrace
) {
    public SemanticComponentModel {
        pageId = safe(pageId);
        componentId = safe(componentId);
        name = safe(name);
        type = type == null ? ComponentType.UNKNOWN : type;
        rootLocatorStrategy = safe(rootLocatorStrategy);
        rootLocatorValue = safe(rootLocatorValue);
        elementIds = elementIds == null ? List.of() : List.copyOf(elementIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
        locators = locators == null ? List.of() : List.copyOf(locators);
        confidence = clamp(confidence);
        risks = risks == null ? List.of() : List.copyOf(risks.stream()
                .filter(risk -> risk != null && !risk.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace.stream()
                .filter(source -> source != null && !source.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d;
    }
}
