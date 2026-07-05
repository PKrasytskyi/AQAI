package ua.demo.agentlab.ui.discovery.mapping.model;

import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public record LocatorCandidate(
        LocatorStrategy strategy,
        String value,
        double stabilityScore,
        String evidenceSource,
        String elementRole,
        String accessibleName,
        String visibleText,
        String href,
        String originHost,
        boolean sameOrigin,
        boolean uniqueOnPage,
        boolean stableAcrossRuns,
        List<String> risks,
        LocatorEvidenceType evidenceType
) {
    public LocatorCandidate(
            LocatorStrategy strategy,
            String value,
            double stabilityScore,
            String evidenceSource,
            String elementRole,
            String accessibleName,
            String visibleText,
            String href,
            String originHost,
            boolean sameOrigin,
            boolean uniqueOnPage,
            boolean stableAcrossRuns,
            List<String> risks
    ) {
        this(
                strategy,
                value,
                stabilityScore,
                evidenceSource,
                elementRole,
                accessibleName,
                visibleText,
                href,
                originHost,
                sameOrigin,
                uniqueOnPage,
                stableAcrossRuns,
                risks,
                inferEvidenceType(stabilityScore, sameOrigin, uniqueOnPage, stableAcrossRuns, risks)
        );
    }

    public LocatorCandidate {
        strategy = strategy == null ? LocatorStrategy.UNKNOWN : strategy;
        value = value == null ? "" : value.trim();
        stabilityScore = Double.isFinite(stabilityScore)
                ? Math.max(0.0d, Math.min(1.0d, stabilityScore))
                : 0.0d;
        evidenceSource = evidenceSource == null ? "" : evidenceSource.trim();
        elementRole = elementRole == null ? "" : elementRole.trim();
        accessibleName = accessibleName == null ? "" : accessibleName.trim();
        visibleText = visibleText == null ? "" : visibleText.trim();
        href = href == null ? "" : href.trim();
        originHost = originHost == null ? "" : originHost.trim().toLowerCase();
        risks = risks == null ? List.of() : List.copyOf(risks.stream()
                .filter(risk -> risk != null && !risk.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
        evidenceType = evidenceType == null ? LocatorEvidenceType.CANDIDATE_LOCATOR : evidenceType;
    }

    private static LocatorEvidenceType inferEvidenceType(
            double score,
            boolean sameOrigin,
            boolean uniqueOnPage,
            boolean stableAcrossRuns,
            List<String> risks
    ) {
        if (score >= 0.75d
                && sameOrigin
                && uniqueOnPage
                && stableAcrossRuns
                && (risks == null || risks.isEmpty())) {
            return LocatorEvidenceType.CONFIRMED_LOCATOR;
        }
        if (score >= 0.45d && sameOrigin) {
            return LocatorEvidenceType.CANDIDATE_LOCATOR;
        }
        return LocatorEvidenceType.FALLBACK_LOCATOR;
    }
}
