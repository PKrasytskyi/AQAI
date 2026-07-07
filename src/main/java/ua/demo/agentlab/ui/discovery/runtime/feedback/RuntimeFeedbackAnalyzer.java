package ua.demo.agentlab.ui.discovery.runtime.feedback;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuntimeFeedbackAnalyzer {

    public RuntimeFeedbackSummary analyze(PageModelBundle pageModelBundle, RuntimeEvidenceBundle runtimeEvidenceBundle) {
        int locatorCandidates = 0;
        int browserVerifiedUnique = 0;
        int unstableLocators = 0;
        Map<String, RuntimeFeedbackIssue> locatorIssues = new LinkedHashMap<>();
        List<RuntimeFeedbackIssue> issues = new ArrayList<>();

        if (pageModelBundle != null) {
            for (PageModel page : pageModelBundle.pages()) {
                for (var element : page.elements()) {
                    for (PageLocatorModel locator : element.locatorCandidates()) {
                        locatorCandidates++;
                        boolean browserUnique = locator.browserMatchCount() == 1 || locator.browserScopedMatchCount() == 1;
                        if (browserUnique) {
                            browserVerifiedUnique++;
                        }
                        if (!locator.stableAcrossRuns()) {
                            unstableLocators++;
                            if (shouldReviewUnstableLocator(locator, browserUnique)) {
                                String evidence = locator.strategy() + "=" + locator.value();
                                locatorIssues.putIfAbsent(
                                        page.pageId() + "|" + evidence,
                                        new RuntimeFeedbackIssue(
                                                "WARN",
                                                "UNSTABLE_LOCATOR",
                                                page.pageId(),
                                                evidence,
                                                "Prefer stable attributes, component-scoped uniqueness, or repeat discovery confirmation"
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }
        issues.addAll(locatorIssues.values());

        int networkFailures = runtimeEvidenceBundle == null ? 0 : (int) runtimeEvidenceBundle.networkResponses().stream()
                .filter(response -> response.status() >= 400)
                .count();
        int consoleErrors = runtimeEvidenceBundle == null ? 0 : (int) runtimeEvidenceBundle.consoleLogs().stream()
                .filter(log -> log.level().equalsIgnoreCase("SEVERE") || log.level().equalsIgnoreCase("ERROR"))
                .count();
        if (networkFailures > 0) {
            issues.add(new RuntimeFeedbackIssue(
                    "WARN",
                    "NETWORK_FAILURES",
                    "",
                    "failed responses=" + networkFailures,
                    "Review whether failed backend calls invalidate page evidence"
            ));
        }
        if (consoleErrors > 0) {
            issues.add(new RuntimeFeedbackIssue(
                    "WARN",
                    "CONSOLE_ERRORS",
                    "",
                    "console errors=" + consoleErrors,
                    "Review browser console failures before promoting page evidence"
            ));
        }

        double locatorPassRate = locatorCandidates == 0 ? 0.0d : (double) browserVerifiedUnique / locatorCandidates;
        double flakyRisk = locatorCandidates == 0 ? 0.0d : (double) unstableLocators / locatorCandidates;
        return new RuntimeFeedbackSummary(
                locatorCandidates,
                browserVerifiedUnique,
                unstableLocators,
                networkFailures,
                consoleErrors,
                runtimeEvidenceBundle == null ? 0 : runtimeEvidenceBundle.semanticNetworkEvidence().size(),
                runtimeEvidenceBundle == null ? 0 : runtimeEvidenceBundle.stateTransitions().size(),
                locatorPassRate,
                flakyRisk,
                issues
        );
    }

    private boolean shouldReviewUnstableLocator(PageLocatorModel locator, boolean browserUnique) {
        if (locator == null || locator.value().isBlank()) {
            return false;
        }
        String value = locator.value().toLowerCase(java.util.Locale.ROOT);
        if (value.equals("body") || value.equals("html") || value.contains(":nth-child(")
                || value.contains(" > div > div")) {
            return false;
        }
        return browserUnique || locator.score() >= 0.75d;
    }
}
