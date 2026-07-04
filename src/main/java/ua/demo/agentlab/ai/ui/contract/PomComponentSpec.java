package ua.demo.agentlab.ai.ui.contract;

import java.util.List;

public record PomComponentSpec(
        String name,
        String type,
        String rootLocatorId,
        List<PomLocatorSpec> locators,
        List<PomActionSpec> actions,
        List<PomAssertionSpec> assertions,
        boolean reusable
) {
    public PomComponentSpec {
        name = safe(name);
        type = safe(type);
        rootLocatorId = safe(rootLocatorId);
        locators = locators == null ? List.of() : List.copyOf(locators);
        actions = actions == null ? List.of() : List.copyOf(actions);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
