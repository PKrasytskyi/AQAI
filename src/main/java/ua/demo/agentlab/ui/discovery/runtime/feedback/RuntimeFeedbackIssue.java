package ua.demo.agentlab.ui.discovery.runtime.feedback;

public record RuntimeFeedbackIssue(
        String severity,
        String issueType,
        String pageId,
        String evidence,
        String recommendation
) {
    public RuntimeFeedbackIssue {
        severity = clean(severity);
        issueType = clean(issueType);
        pageId = clean(pageId);
        evidence = clean(evidence);
        recommendation = clean(recommendation);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
