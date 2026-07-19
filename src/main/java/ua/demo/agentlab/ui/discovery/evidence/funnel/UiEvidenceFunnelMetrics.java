package ua.demo.agentlab.ui.discovery.evidence.funnel;

/** Counts each evidence boundary separately so raw discovery cannot masquerade as prompt readiness. */
public record UiEvidenceFunnelMetrics(
        int rawLocatorCandidates,
        int genericInventoryLocatorCandidates,
        int liveVerifiedLocators,
        int documentNavigationTransitionsVerified,
        int spaStateTransitionsVerified,
        int dbStableLocators,
        int contextPromptAllowedLocators,
        int requirementScopedPromptAllowedLocators,
        int requirementBoundPages,
        int promptEligiblePages
) {
    public UiEvidenceFunnelMetrics {
        rawLocatorCandidates = nonNegative(rawLocatorCandidates);
        genericInventoryLocatorCandidates = nonNegative(genericInventoryLocatorCandidates);
        liveVerifiedLocators = nonNegative(liveVerifiedLocators);
        documentNavigationTransitionsVerified = nonNegative(documentNavigationTransitionsVerified);
        spaStateTransitionsVerified = nonNegative(spaStateTransitionsVerified);
        dbStableLocators = nonNegative(dbStableLocators);
        contextPromptAllowedLocators = nonNegative(contextPromptAllowedLocators);
        requirementScopedPromptAllowedLocators = nonNegative(requirementScopedPromptAllowedLocators);
        requirementBoundPages = nonNegative(requirementBoundPages);
        promptEligiblePages = nonNegative(promptEligiblePages);
    }

    public UiEvidenceFunnelMetrics(
            int rawLocatorCandidates,
            int liveVerifiedLocators,
            int dbStableLocators,
            int contextPromptAllowedLocators,
            int requirementScopedPromptAllowedLocators,
            int requirementBoundPages,
            int promptEligiblePages
    ) {
        this(rawLocatorCandidates, rawLocatorCandidates, liveVerifiedLocators, 0, 0, dbStableLocators,
                contextPromptAllowedLocators, requirementScopedPromptAllowedLocators,
                requirementBoundPages, promptEligiblePages);
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }
}
