package ua.demo.agentlab.ai.ui.contract;

public record PomContractIssue(
        PomContractSeverity severity,
        String ruleId,
        String message,
        String evidence
) {
    public PomContractIssue {
        severity = severity == null ? PomContractSeverity.WARNING : severity;
        ruleId = safe(ruleId);
        message = safe(message);
        evidence = safe(evidence);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
