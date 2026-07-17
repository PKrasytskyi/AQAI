package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

/** A deterministic prerequisite relation between two component-scoped actions. */
public record ComponentActionDependency(
        String pageId,
        String componentId,
        String prerequisiteActionId,
        String dependentActionId,
        String condition,
        double confidence,
        List<String> sourceTrace
) {
    public ComponentActionDependency {
        pageId = safe(pageId);
        componentId = safe(componentId);
        prerequisiteActionId = safe(prerequisiteActionId);
        dependentActionId = safe(dependentActionId);
        condition = safe(condition);
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
