package ua.demo.agentlab.api.quality;

import java.util.List;

public record ApiQualityReport(
        String scope,
        List<ApiQualityIssue> issues
) {
    public ApiQualityReport {
        scope = scope == null ? "" : scope.trim();
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean hasBlockingIssues() {
        return issues.stream().anyMatch(issue -> issue.severity() == ApiQualitySeverity.BLOCKER);
    }

    public long blockingIssueCount() {
        return issues.stream().filter(issue -> issue.severity() == ApiQualitySeverity.BLOCKER).count();
    }
}
