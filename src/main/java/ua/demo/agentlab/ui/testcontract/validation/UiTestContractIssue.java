package ua.demo.agentlab.ui.testcontract.validation;

public record UiTestContractIssue(
        UiTestContractIssueSeverity severity,
        String ruleId,
        String scenarioId,
        String message,
        String evidence
) {
    public UiTestContractIssue {
        severity = severity == null ? UiTestContractIssueSeverity.BLOCKER : severity;
        ruleId = safe(ruleId);
        scenarioId = safe(scenarioId);
        message = safe(message);
        evidence = safe(evidence);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
