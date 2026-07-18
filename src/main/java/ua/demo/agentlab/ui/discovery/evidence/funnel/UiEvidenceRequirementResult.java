package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.List;

/** Explains how one requirement reached POM evidence or exactly where it stopped. */
public record UiEvidenceRequirementResult(
        String requirementId,
        String capability,
        UiEvidenceRequirementTrace trace,
        String pageId,
        String route,
        int rawLocatorCandidates,
        int liveVerifiedLocators,
        int dbStableLocators,
        int promptAllowedLocators,
        boolean requirementBound,
        boolean promptEligible,
        boolean confirmedEvidencePath,
        List<String> evidencePath,
        String stoppedAt,
        String reason,
        String remediation
) {
    public UiEvidenceRequirementResult {
        requirementId = safe(requirementId);
        capability = safe(capability);
        trace = trace == null ? UiEvidenceRequirementTrace.empty() : trace;
        pageId = safe(pageId);
        route = safe(route);
        rawLocatorCandidates = Math.max(0, rawLocatorCandidates);
        liveVerifiedLocators = Math.max(0, liveVerifiedLocators);
        dbStableLocators = Math.max(0, dbStableLocators);
        promptAllowedLocators = Math.max(0, promptAllowedLocators);
        evidencePath = evidencePath == null ? List.of() : List.copyOf(evidencePath);
        stoppedAt = safe(stoppedAt);
        reason = safe(reason);
        remediation = safe(remediation);
    }

    public UiEvidenceRequirementResult(
            String requirementId,
            String capability,
            String pageId,
            String route,
            int rawLocatorCandidates,
            int liveVerifiedLocators,
            int dbStableLocators,
            int promptAllowedLocators,
            boolean requirementBound,
            boolean promptEligible,
            boolean confirmedEvidencePath,
            List<String> evidencePath,
            String stoppedAt,
            String reason,
            String remediation
    ) {
        this(requirementId, capability, UiEvidenceRequirementTrace.empty(), pageId, route,
                rawLocatorCandidates, liveVerifiedLocators, dbStableLocators, promptAllowedLocators,
                requirementBound, promptEligible, confirmedEvidencePath, evidencePath,
                stoppedAt, reason, remediation);
    }

    public boolean hasExplicitStop() {
        return !stoppedAt.isBlank() && !reason.isBlank() && !remediation.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
