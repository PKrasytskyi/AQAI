package ua.demo.agentlab.api.quality;

public record ApiQualityIssue(
        ApiQualitySeverity severity,
        String ruleId,
        String message,
        String evidence
) {
    public ApiQualityIssue {
        severity = severity == null ? ApiQualitySeverity.WARNING : severity;
        ruleId = safe(ruleId);
        message = safe(message);
        evidence = safe(evidence);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
