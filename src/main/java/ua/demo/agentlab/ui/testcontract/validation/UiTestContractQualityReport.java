package ua.demo.agentlab.ui.testcontract.validation;

import java.util.List;

public record UiTestContractQualityReport(
        String schemaVersion,
        int contracts,
        int readyContracts,
        int needsReviewContracts,
        List<UiTestContractIssue> issues
) {
    public UiTestContractQualityReport {
        schemaVersion = schemaVersion == null ? "" : schemaVersion.trim();
        contracts = Math.max(0, contracts);
        readyContracts = Math.max(0, readyContracts);
        needsReviewContracts = Math.max(0, needsReviewContracts);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean hasBlockingIssues() {
        return issues.stream().anyMatch(issue -> issue.severity() == UiTestContractIssueSeverity.BLOCKER);
    }
}
