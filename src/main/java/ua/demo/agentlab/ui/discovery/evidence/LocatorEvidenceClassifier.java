package ua.demo.agentlab.ui.discovery.evidence;

import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.util.List;
import java.util.Locale;

public class LocatorEvidenceClassifier {

    private static final double CONFIRMED_SCORE = 0.75d;
    private static final double CANDIDATE_SCORE = 0.45d;

    public LocatorEvidenceType classify(LocatorCandidate locator) {
        if (locator == null || locator.value().isBlank()) {
            return LocatorEvidenceType.FALLBACK_LOCATOR;
        }
        if (confirmed(locator.stabilityScore(), locator.sameOrigin(), locator.uniqueOnPage(),
                locator.stableAcrossRuns(), true, locator.risks())) {
            return LocatorEvidenceType.CONFIRMED_LOCATOR;
        }
        if (candidate(locator.stabilityScore(), locator.sameOrigin(), locator.risks())) {
            return LocatorEvidenceType.CANDIDATE_LOCATOR;
        }
        return LocatorEvidenceType.FALLBACK_LOCATOR;
    }

    public LocatorEvidenceType classify(ScopedLocatorCandidate locator) {
        if (locator == null || locator.value().isBlank()) {
            return LocatorEvidenceType.FALLBACK_LOCATOR;
        }
        boolean browserVerified = locator.globalMatchCount() >= 0 && locator.scopedMatchCount() >= 0;
        if (confirmed(locator.finalScore(), true, locator.uniqueWithinComponent(),
                true, browserVerified, locator.risks())) {
            return LocatorEvidenceType.CONFIRMED_LOCATOR;
        }
        if (candidate(locator.finalScore(), true, locator.risks())) {
            return LocatorEvidenceType.CANDIDATE_LOCATOR;
        }
        return LocatorEvidenceType.FALLBACK_LOCATOR;
    }

    public LocatorEvidenceType classify(PageLocatorModel locator) {
        if (locator == null || locator.value().isBlank()) {
            return LocatorEvidenceType.FALLBACK_LOCATOR;
        }
        boolean browserVerified = locator.browserMatchCount() >= 0 && locator.browserScopedMatchCount() >= 0;
        if (confirmed(locator.score(), true, locator.unique(), locator.stableAcrossRuns(),
                browserVerified, List.of())) {
            return LocatorEvidenceType.CONFIRMED_LOCATOR;
        }
        if (locator.score() >= CANDIDATE_SCORE) {
            return LocatorEvidenceType.CANDIDATE_LOCATOR;
        }
        return LocatorEvidenceType.FALLBACK_LOCATOR;
    }

    public LocatorEvidenceType classify(POMRelevantEvidence evidence) {
        if (evidence == null || evidence.value().isBlank()) {
            return LocatorEvidenceType.FALLBACK_LOCATOR;
        }
        boolean browserVerified = evidence.globalMatchCount() >= 0 && evidence.scopedMatchCount() >= 0;
        if (confirmed(evidence.finalScore(), evidence.sameOrigin(), evidence.uniqueWithinComponent(),
                true, browserVerified, evidence.risks())) {
            return LocatorEvidenceType.CONFIRMED_LOCATOR;
        }
        if (candidate(evidence.finalScore(), evidence.sameOrigin(), evidence.risks())) {
            return LocatorEvidenceType.CANDIDATE_LOCATOR;
        }
        return LocatorEvidenceType.FALLBACK_LOCATOR;
    }

    public boolean promptAllowed(LocatorEvidenceType type) {
        return type == LocatorEvidenceType.CONFIRMED_LOCATOR;
    }

    private boolean confirmed(
            double score,
            boolean sameOrigin,
            boolean unique,
            boolean stableAcrossRuns,
            boolean browserVerified,
            List<String> risks
    ) {
        return score >= CONFIRMED_SCORE
                && sameOrigin
                && unique
                && stableAcrossRuns
                && browserVerified
                && noBlockingRisk(risks);
    }

    private boolean candidate(double score, boolean sameOrigin, List<String> risks) {
        return score >= CANDIDATE_SCORE
                && sameOrigin
                && noForbiddenRisk(risks);
    }

    private boolean noBlockingRisk(List<String> risks) {
        return risks == null || risks.stream().noneMatch(this::blockingRisk);
    }

    private boolean noForbiddenRisk(List<String> risks) {
        return risks == null || risks.stream().noneMatch(this::forbiddenRisk);
    }

    private boolean blockingRisk(String risk) {
        String normalized = normalize(risk);
        return forbiddenRisk(normalized)
                || normalized.contains("unstable")
                || normalized.contains("not-proven")
                || normalized.contains("browser-global-count-missing")
                || normalized.contains("browser-scoped-count-missing")
                || normalized.contains("low-confidence");
    }

    private boolean forbiddenRisk(String risk) {
        String normalized = normalize(risk);
        return normalized.contains("external")
                || normalized.contains("security-token")
                || normalized.contains("hidden-or-invisible")
                || normalized.contains("semantic-locator-conflict")
                || normalized.contains("absolute-dom-path")
                || normalized.contains("long-absolute-xpath")
                || normalized.contains("dynamic-css-hash")
                || normalized.contains("framework-generated-class");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
