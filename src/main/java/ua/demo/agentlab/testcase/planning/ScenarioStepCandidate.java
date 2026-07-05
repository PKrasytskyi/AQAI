package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.ui.contract.UiOperationKind;

public record ScenarioStepCandidate(
        UiOperationKind kind,
        String ownerPage,
        String route,
        String dataKey,
        boolean setup
) {
    public ScenarioStepCandidate {
        if (kind == null) {
            throw new IllegalArgumentException("kind cannot be null");
        }
        ownerPage = ownerPage == null ? "" : ownerPage.trim();
        route = route == null ? "" : route.trim();
        dataKey = dataKey == null || dataKey.isBlank() ? null : dataKey.trim();
    }
}
