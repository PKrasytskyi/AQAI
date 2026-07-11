package ua.demo.agentlab.ai.quality;

public record AiRunQualitySummary(
        String runId,
        int requirements,
        int canonicalTestCases,
        int expectedResultsResolved,
        int expectedResultsNeedsReview,
        int mappedPages,
        int pageObjectPrompts,
        int promptPagesWithoutAllowedLocators,
        int routeCollisions,
        int externalEvidenceRejected,
        int lowConfidenceLocators,
        int confirmedLocators,
        int candidateLocators,
        int fallbackLocators,
        int promptBlockingIssues,
        double averageLocatorScore,
        boolean neo4jHit,
        boolean qdrantHit,
        String retrievalMode,
        boolean stableCacheUsed,
        String dbUsageMode,
        int pageEnrichmentGenerated,
        int pageEnrichmentCacheHits,
        int pageEnrichmentOpenAiCalls,
        int pageEnrichmentOpenAiAttempts,
        int pageEnrichmentOpenAiSuccesses,
        int pageEnrichmentOpenAiFailures,
        int pageEnrichmentOpenAiFallbacks,
        int staleEvidenceRejected,
        String vectorUnavailableReason,
        int qualityScore
) {
    public AiRunQualitySummary {
        runId = runId == null || runId.isBlank() ? "unknown-run" : runId.trim();
        requirements = Math.max(0, requirements);
        canonicalTestCases = Math.max(0, canonicalTestCases);
        expectedResultsResolved = Math.max(0, expectedResultsResolved);
        expectedResultsNeedsReview = Math.max(0, expectedResultsNeedsReview);
        mappedPages = Math.max(0, mappedPages);
        pageObjectPrompts = Math.max(0, pageObjectPrompts);
        promptPagesWithoutAllowedLocators = Math.max(0, promptPagesWithoutAllowedLocators);
        routeCollisions = Math.max(0, routeCollisions);
        externalEvidenceRejected = Math.max(0, externalEvidenceRejected);
        lowConfidenceLocators = Math.max(0, lowConfidenceLocators);
        confirmedLocators = Math.max(0, confirmedLocators);
        candidateLocators = Math.max(0, candidateLocators);
        fallbackLocators = Math.max(0, fallbackLocators);
        promptBlockingIssues = Math.max(0, promptBlockingIssues);
        averageLocatorScore = Double.isFinite(averageLocatorScore)
                ? Math.max(0.0d, Math.min(1.0d, averageLocatorScore))
                : 0.0d;
        retrievalMode = retrievalMode == null || retrievalMode.isBlank() ? "unknown" : retrievalMode.trim();
        dbUsageMode = dbUsageMode == null || dbUsageMode.isBlank() ? "unknown" : dbUsageMode.trim();
        pageEnrichmentGenerated = Math.max(0, pageEnrichmentGenerated);
        pageEnrichmentCacheHits = Math.max(0, pageEnrichmentCacheHits);
        pageEnrichmentOpenAiCalls = Math.max(0, pageEnrichmentOpenAiCalls);
        pageEnrichmentOpenAiAttempts = Math.max(0, pageEnrichmentOpenAiAttempts);
        pageEnrichmentOpenAiSuccesses = Math.max(0, pageEnrichmentOpenAiSuccesses);
        pageEnrichmentOpenAiFailures = Math.max(0, pageEnrichmentOpenAiFailures);
        pageEnrichmentOpenAiFallbacks = Math.max(0, pageEnrichmentOpenAiFallbacks);
        staleEvidenceRejected = Math.max(0, staleEvidenceRejected);
        vectorUnavailableReason = vectorUnavailableReason == null ? "" : vectorUnavailableReason.trim();
        qualityScore = Math.max(0, Math.min(100, qualityScore));
    }
}
