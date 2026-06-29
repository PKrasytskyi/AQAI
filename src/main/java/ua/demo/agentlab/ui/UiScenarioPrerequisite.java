package ua.demo.agentlab.ui;

import java.util.List;

public record UiScenarioPrerequisite(
        String sourcePageName,
        String sourceRoute,
        boolean authenticationRequired,
        List<String> setupActions
) {

    public UiScenarioPrerequisite {
        sourcePageName = blankToNull(sourcePageName);
        sourceRoute = blankToNull(sourceRoute);
        setupActions = setupActions == null ? List.of() : List.copyOf(setupActions);
    }

    public boolean hasSourcePage() {
        return sourcePageName != null;
    }

    public boolean hasSetupActions() {
        return !setupActions.isEmpty();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
