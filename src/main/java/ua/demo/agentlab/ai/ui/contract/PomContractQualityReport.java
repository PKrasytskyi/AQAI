package ua.demo.agentlab.ai.ui.contract;

import java.util.List;

public record PomContractQualityReport(
        String schemaVersion,
        String pageName,
        List<PomContractIssue> issues
) {
    public PomContractQualityReport {
        schemaVersion = schemaVersion == null ? "" : schemaVersion.trim();
        pageName = pageName == null ? "" : pageName.trim();
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean hasBlockingIssues() {
        return issues.stream().anyMatch(issue -> issue.severity() == PomContractSeverity.BLOCKER);
    }

    public long blockingIssueCount() {
        return issues.stream().filter(issue -> issue.severity() == PomContractSeverity.BLOCKER).count();
    }
}
