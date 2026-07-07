package ua.demo.agentlab.validation.smoke;

public record GeneratedUiSmokeIssue(
        String severity,
        String ruleId,
        String filePath,
        String message
) {
    public GeneratedUiSmokeIssue {
        severity = clean(severity);
        ruleId = clean(ruleId);
        filePath = clean(filePath);
        message = clean(message);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
