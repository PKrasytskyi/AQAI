package ua.demo.agentlab.ai.ui.prompt.quality;

public record PromptQualityIssue(
        PromptQualitySeverity severity,
        String ruleId,
        String message,
        String evidence
) {
    public PromptQualityIssue {
        severity = severity == null ? PromptQualitySeverity.WARNING : severity;
        ruleId = safe(ruleId);
        message = safe(message);
        evidence = safe(evidence);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
