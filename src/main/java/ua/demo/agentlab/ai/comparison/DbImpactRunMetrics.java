package ua.demo.agentlab.ai.comparison;

public record DbImpactRunMetrics(
        String runId,
        String runRoot,
        int totalTokens,
        boolean tokenUsageEstimated,
        int llmCalls,
        int averagePromptSize,
        int reusedPageKnowledgeArtifacts,
        int repeatedContextFragments,
        int qualityScore,
        int lowConfidenceLocators,
        int expectedResultsForReview,
        int promptSafetyBlocks,
        int validPomContracts,
        int totalPomContracts,
        int compileReadyGeneratedCode,
        int totalGeneratedCode,
        int artifactDiffSize,
        boolean neo4jHit,
        boolean qdrantHit,
        boolean stableCacheUsed,
        String retrievalMode,
        String dbUsageMode,
        int pageEnrichmentGenerated,
        int pageEnrichmentCacheHits,
        int pageEnrichmentOpenAiCalls
) {
    public DbImpactRunMetrics {
        runId = runId == null || runId.isBlank() ? "unknown-run" : runId.trim();
        runRoot = runRoot == null ? "" : runRoot.trim();
        totalTokens = Math.max(0, totalTokens);
        llmCalls = Math.max(0, llmCalls);
        averagePromptSize = Math.max(0, averagePromptSize);
        reusedPageKnowledgeArtifacts = Math.max(0, reusedPageKnowledgeArtifacts);
        repeatedContextFragments = Math.max(0, repeatedContextFragments);
        qualityScore = Math.max(0, Math.min(100, qualityScore));
        lowConfidenceLocators = Math.max(0, lowConfidenceLocators);
        expectedResultsForReview = Math.max(0, expectedResultsForReview);
        promptSafetyBlocks = Math.max(0, promptSafetyBlocks);
        validPomContracts = Math.max(0, validPomContracts);
        totalPomContracts = Math.max(0, totalPomContracts);
        compileReadyGeneratedCode = Math.max(0, compileReadyGeneratedCode);
        totalGeneratedCode = Math.max(0, totalGeneratedCode);
        artifactDiffSize = Math.max(0, artifactDiffSize);
        pageEnrichmentGenerated = Math.max(0, pageEnrichmentGenerated);
        pageEnrichmentCacheHits = Math.max(0, pageEnrichmentCacheHits);
        pageEnrichmentOpenAiCalls = Math.max(0, pageEnrichmentOpenAiCalls);
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
