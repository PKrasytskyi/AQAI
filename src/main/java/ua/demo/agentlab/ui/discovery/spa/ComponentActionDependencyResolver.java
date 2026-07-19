package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.spa.model.ComponentActionDependency;

import java.util.Locale;
import java.util.Map;

/** Resolves duplicate discovery action IDs as alternatives for one semantic prerequisite. */
public final class ComponentActionDependencyResolver {

    public boolean passed(ComponentActionDependency dependency, Map<String, Boolean> outcomes) {
        if (dependency == null || outcomes == null) return false;
        if (Boolean.TRUE.equals(outcomes.get(dependency.prerequisiteActionId()))) return true;
        String semanticId = semanticActionId(dependency.prerequisiteActionId());
        return outcomes.entrySet().stream()
                .anyMatch(entry -> Boolean.TRUE.equals(entry.getValue())
                        && semanticActionId(entry.getKey()).equals(semanticId));
    }

    String semanticActionId(String actionId) {
        String value = actionId == null ? "" : actionId.trim().toLowerCase(Locale.ROOT);
        return value.replaceFirst("-\\d+$", "");
    }
}
