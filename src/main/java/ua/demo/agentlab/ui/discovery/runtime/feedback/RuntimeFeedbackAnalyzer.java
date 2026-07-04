package ua.demo.agentlab.ui.discovery.runtime.feedback;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;

import java.util.ArrayList;
import java.util.List;

public class RuntimeFeedbackAnalyzer {

    public RuntimeFeedbackSummary analyze(PageModelBundle pageModelBundle, RuntimeEvidenceBundle runtimeEvidenceBundle) {
        int locatorCandidates = 0;
        int browserVerifiedUnique = 0;
        int unstableLocators = 0;
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
                            issues.add(new RuntimeFeedbackIssue(
                                    "WARN",
                                    "UNSTABLE_LOCATOR",
                                    page.pageId(),
                                    locator.strategy() + "=" + locator.value(),
                                    "Prefer stable attributes, component-scoped uniqueness, or repeat discovery confirmation"
                            ));
                        }
                    }
                }
            }
        }

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
}
