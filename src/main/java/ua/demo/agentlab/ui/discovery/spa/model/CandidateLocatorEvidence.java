package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public record CandidateLocatorEvidence(
        String locatorId,
        String componentId,
        String elementId,
        String strategy,
        String value,
        double qualityScore,
        boolean sameOrigin,
        int globalMatchCount,
        int componentMatchCount,
        boolean uniqueOnPage,
        boolean uniqueWithinComponent,
        boolean stableAcrossRuns,
        LocatorEvidenceType observedEvidenceType,
        SpaEvidenceStatus status,
        List<String> risks
) {
    public CandidateLocatorEvidence {
        locatorId = safe(locatorId);
        componentId = safe(componentId);
        elementId = safe(elementId);
        strategy = safe(strategy);
        value = safe(value);
        qualityScore = clamp(qualityScore);
        globalMatchCount = Math.max(0, globalMatchCount);
        componentMatchCount = Math.max(0, componentMatchCount);
        observedEvidenceType = observedEvidenceType == null ? LocatorEvidenceType.CANDIDATE_LOCATOR : observedEvidenceType;
        status = status == null ? SpaEvidenceStatus.CANDIDATE : status;
        risks = risks == null ? List.of() : List.copyOf(risks);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static double clamp(double value) { return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d; }
}
