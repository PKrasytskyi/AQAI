package ua.demo.agentlab.ai.comparison;

public record DbImpactRunMetrics(
        String runId,
        String runRoot,
        int totalTokens,
        int promptTokens,
        int responseTokens,
        int actualTokens,
        boolean tokenUsageEstimated,
        String tokenCountingMode,
        String tokenizerModel,
        int llmCalls,
        int llmSuccessfulCalls,
        int llmFailedCalls,
        int pomLlmCalls,
        int averagePromptSize,
        int reusedPageKnowledgeArtifacts,
        int repeatedContextFragments,
        int qualityScore,
        int lowConfidenceLocators,
        int expectedResultsForReview,
        int promptSafetyBlocks,
        int validPomContracts,
        int totalPomContracts,
        int newlyValidPomContracts,
        int newlyTotalPomContracts,
        int reusedValidPomContracts,
        int reusedTotalPomContracts,
        int compileReadyGeneratedCode,
        int totalGeneratedCode,
        String compileStatus,
        int artifactDiffSize,
        boolean neo4jHit,
        boolean qdrantHit,
        boolean stableCacheUsed,
        String retrievalMode,
        String dbUsageMode,
        int pageEnrichmentGenerated,
        int pageEnrichmentCacheHits,
        int pageEnrichmentOpenAiCalls,
        int pageEnrichmentOpenAiAttempts,
        int pageEnrichmentOpenAiSuccesses,
        int pageEnrichmentOpenAiFailures,
        int pageEnrichmentOpenAiFallbacks,
        boolean artifactReuseEnabled,
        int artifactReuseLlmCallsExecuted,
        int artifactReuseLlmCallsSkipped,
        int artifactCacheHits,
        int artifactCacheMisses,
        int artifactTokensSavedEstimate,
        int stableArtifactCount,
        int artifactNeedsReviewCount,
        int stableLocatorReuse,
        int flowReuse
) {
    public DbImpactRunMetrics {
        runId = runId == null || runId.isBlank() ? "unknown-run" : runId.trim();
        runRoot = runRoot == null ? "" : runRoot.trim();
        totalTokens = Math.max(0, totalTokens);
        promptTokens = Math.max(0, promptTokens);
        responseTokens = Math.max(0, responseTokens);
        actualTokens = Math.max(0, actualTokens);
        tokenCountingMode = clean(tokenCountingMode, "unknown");
        tokenizerModel = clean(tokenizerModel, "unknown");
        llmCalls = Math.max(0, llmCalls);
        llmSuccessfulCalls = Math.max(0, llmSuccessfulCalls);
        llmFailedCalls = Math.max(0, llmFailedCalls);
        pomLlmCalls = Math.max(0, pomLlmCalls);
        averagePromptSize = Math.max(0, averagePromptSize);
        reusedPageKnowledgeArtifacts = Math.max(0, reusedPageKnowledgeArtifacts);
        repeatedContextFragments = Math.max(0, repeatedContextFragments);
        qualityScore = Math.max(0, Math.min(100, qualityScore));
        lowConfidenceLocators = Math.max(0, lowConfidenceLocators);
        expectedResultsForReview = Math.max(0, expectedResultsForReview);
        promptSafetyBlocks = Math.max(0, promptSafetyBlocks);
        validPomContracts = Math.max(0, validPomContracts);
        totalPomContracts = Math.max(0, totalPomContracts);
        newlyValidPomContracts = Math.max(0, newlyValidPomContracts);
        newlyTotalPomContracts = Math.max(0, newlyTotalPomContracts);
        reusedValidPomContracts = Math.max(0, reusedValidPomContracts);
        reusedTotalPomContracts = Math.max(0, reusedTotalPomContracts);
        compileReadyGeneratedCode = Math.max(0, compileReadyGeneratedCode);
        totalGeneratedCode = Math.max(0, totalGeneratedCode);
        compileStatus = clean(compileStatus, "unknown");
        artifactDiffSize = Math.max(0, artifactDiffSize);
        pageEnrichmentGenerated = Math.max(0, pageEnrichmentGenerated);
        pageEnrichmentCacheHits = Math.max(0, pageEnrichmentCacheHits);
        pageEnrichmentOpenAiCalls = Math.max(0, pageEnrichmentOpenAiCalls);
        pageEnrichmentOpenAiAttempts = Math.max(0, pageEnrichmentOpenAiAttempts);
        pageEnrichmentOpenAiSuccesses = Math.max(0, pageEnrichmentOpenAiSuccesses);
        pageEnrichmentOpenAiFailures = Math.max(0, pageEnrichmentOpenAiFailures);
        pageEnrichmentOpenAiFallbacks = Math.max(0, pageEnrichmentOpenAiFallbacks);
        artifactReuseLlmCallsExecuted = Math.max(0, artifactReuseLlmCallsExecuted);
        artifactReuseLlmCallsSkipped = Math.max(0, artifactReuseLlmCallsSkipped);
        artifactCacheHits = Math.max(0, artifactCacheHits);
        artifactCacheMisses = Math.max(0, artifactCacheMisses);
        artifactTokensSavedEstimate = Math.max(0, artifactTokensSavedEstimate);
        stableArtifactCount = Math.max(0, stableArtifactCount);
        artifactNeedsReviewCount = Math.max(0, artifactNeedsReviewCount);
        stableLocatorReuse = Math.max(0, stableLocatorReuse);
        flowReuse = Math.max(0, flowReuse);
        retrievalMode = clean(retrievalMode, "unknown");
        dbUsageMode = clean(dbUsageMode, "unknown");
    }

    public boolean fullDbRun() {
        return neo4jHit && qdrantHit && stableCacheUsed;
    }

    public boolean withoutDbRun() {
        return !neo4jHit && !qdrantHit && !stableCacheUsed;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
