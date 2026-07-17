package ua.demo.agentlab.ui.discovery.evidence.funnel;

/** Counts each evidence boundary separately so raw discovery cannot masquerade as prompt readiness. */
public record UiEvidenceFunnelMetrics(
        int rawLocatorCandidates,
        int liveVerifiedLocators,
        int dbStableLocators,
        int contextPromptAllowedLocators,
        int requirementScopedPromptAllowedLocators,
        int requirementBoundPages,
        int promptEligiblePages
) {
    public UiEvidenceFunnelMetrics {
        rawLocatorCandidates = nonNegative(rawLocatorCandidates);
        liveVerifiedLocators = nonNegative(liveVerifiedLocators);
        dbStableLocators = nonNegative(dbStableLocators);
        contextPromptAllowedLocators = nonNegative(contextPromptAllowedLocators);
        requirementScopedPromptAllowedLocators = nonNegative(requirementScopedPromptAllowedLocators);
        requirementBoundPages = nonNegative(requirementBoundPages);
        promptEligiblePages = nonNegative(promptEligiblePages);
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }
}
