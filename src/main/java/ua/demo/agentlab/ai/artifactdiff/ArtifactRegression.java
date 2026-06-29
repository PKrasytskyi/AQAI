package ua.demo.agentlab.ai.artifactdiff;

public record ArtifactRegression(
        String category,
        String ruleId,
        String message,
        String previousValue,
        String currentValue
) {
    public ArtifactRegression {
        category = safe(category);
        ruleId = safe(ruleId);
        message = safe(message);
        previousValue = safe(previousValue);
        currentValue = safe(currentValue);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
