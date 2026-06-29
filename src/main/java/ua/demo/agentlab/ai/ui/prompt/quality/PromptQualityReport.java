package ua.demo.agentlab.ai.ui.prompt.quality;

import java.util.List;

public record PromptQualityReport(
        String promptType,
        String targetPage,
        String targetRoute,
        List<String> requirementIds,
        List<PromptQualityIssue> issues
) {
    public PromptQualityReport {
        promptType = safe(promptType);
        targetPage = safe(targetPage);
        targetRoute = safe(targetRoute);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean hasBlockingIssues() {
        return issues.stream().anyMatch(issue -> issue.severity() == PromptQualitySeverity.BLOCKER);
    }

    public long blockingIssueCount() {
        return issues.stream().filter(issue -> issue.severity() == PromptQualitySeverity.BLOCKER).count();
    }

    public long warningIssueCount() {
        return issues.stream().filter(issue -> issue.severity() == PromptQualitySeverity.WARNING).count();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
