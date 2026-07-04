package ua.demo.agentlab.ui.discovery.runtime.feedback;

import java.util.List;

public record RuntimeFeedbackSummary(
        int locatorCandidates,
        int browserVerifiedUniqueLocators,
        int unstableLocators,
        int networkFailures,
        int consoleErrors,
        int semanticNetworkFacts,
        int stateTransitions,
        double locatorPassRate,
        double flakyRiskScore,
        List<RuntimeFeedbackIssue> issues
) {
    public RuntimeFeedbackSummary {
        locatorCandidates = Math.max(0, locatorCandidates);
        browserVerifiedUniqueLocators = Math.max(0, browserVerifiedUniqueLocators);
        unstableLocators = Math.max(0, unstableLocators);
        networkFailures = Math.max(0, networkFailures);
        consoleErrors = Math.max(0, consoleErrors);
        semanticNetworkFacts = Math.max(0, semanticNetworkFacts);
        stateTransitions = Math.max(0, stateTransitions);
        locatorPassRate = Math.max(0.0d, Math.min(1.0d, locatorPassRate));
        flakyRiskScore = Math.max(0.0d, Math.min(1.0d, flakyRiskScore));
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
