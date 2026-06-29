package ua.demo.agentlab.ai.ui.prompt.quality;

public class PromptQualityGateException extends RuntimeException {

    private final PromptQualityReport report;

    public PromptQualityGateException(PromptQualityReport report) {
        super(message(report));
        this.report = report;
    }

    public PromptQualityReport report() {
        return report;
    }

    private static String message(PromptQualityReport report) {
        if (report == null) {
            return "Prompt quality gate failed";
        }
        return "Prompt quality gate failed for " + report.targetPage()
                + " with " + report.blockingIssueCount() + " blocking issue(s)";
    }
}
