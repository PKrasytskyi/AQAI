package ua.demo.agentlab.ai.ui.contract;

public record PomCheckSpec(
        PomCheckType check,
        String locator,
        String expectedValue,
        String valueFrom,
        String attribute,
        String route
) {
    public PomCheckSpec {
        check = check == null ? PomCheckType.VISIBLE : check;
        locator = safe(locator);
        expectedValue = safe(expectedValue);
        valueFrom = safe(valueFrom);
        attribute = safe(attribute);
        route = safe(route);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
