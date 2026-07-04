package ua.demo.agentlab.ai.ui.contract;

public record PomStepSpec(
        PomStepAction action,
        String locator,
        String valueFrom,
        String literalValue,
        String route
) {
    public PomStepSpec {
        action = action == null ? PomStepAction.CLICK : action;
        locator = safe(locator);
        valueFrom = safe(valueFrom);
        literalValue = safe(literalValue);
        route = safe(route);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
