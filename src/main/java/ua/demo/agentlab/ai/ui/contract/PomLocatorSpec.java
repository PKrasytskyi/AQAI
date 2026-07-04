package ua.demo.agentlab.ai.ui.contract;

public record PomLocatorSpec(
        String id,
        String elementName,
        String strategy,
        String value,
        String role,
        double stabilityScore
) {
    public PomLocatorSpec {
        id = safe(id);
        elementName = safe(elementName);
        strategy = safe(strategy);
        value = safe(value);
        role = safe(role);
        stabilityScore = Math.max(0.0d, Math.min(1.0d, stabilityScore));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
