package ua.demo.agentlab.artifactreuse.metrics;

/** Compact, final-state view of one completed AI run for release and demo review. */
public record RunHistoryStatisticsRow(
        String runId,
        int requirements,
        int canonicalTestCases,
        int qualityScore,
        String dbRetrieval,
        int pomReuseHits,
        int pomReuseMisses,
        int flowReuseHits,
        int flowReuseMisses,
        int llmCallsExecuted,
        int llmCallsSkipped,
        int lifecycleStable,
        int lifecycleNeedsReview,
        String compileStatus,
        int reviewFindings,
        String generatedSmokeStatus,
        String liveSmokeStatus,
        String flowFeedback,
        String outcome
) {
    public RunHistoryStatisticsRow {
        runId = safe(runId, "unknown-run");
        dbRetrieval = safe(dbRetrieval, "unavailable");
        compileStatus = safe(compileStatus, "missing-artifact");
        generatedSmokeStatus = safe(generatedSmokeStatus, "missing-artifact");
        liveSmokeStatus = safe(liveSmokeStatus, "missing-artifact");
        flowFeedback = safe(flowFeedback, "missing-artifact");
        outcome = safe(outcome, "WARNING");
        requirements = Math.max(0, requirements);
        canonicalTestCases = Math.max(0, canonicalTestCases);
        qualityScore = Math.max(0, Math.min(100, qualityScore));
        pomReuseHits = Math.max(0, pomReuseHits);
        pomReuseMisses = Math.max(0, pomReuseMisses);
        flowReuseHits = Math.max(0, flowReuseHits);
        flowReuseMisses = Math.max(0, flowReuseMisses);
        llmCallsExecuted = Math.max(0, llmCallsExecuted);
        llmCallsSkipped = Math.max(0, llmCallsSkipped);
        lifecycleStable = Math.max(0, lifecycleStable);
        lifecycleNeedsReview = Math.max(0, lifecycleNeedsReview);
        reviewFindings = Math.max(0, reviewFindings);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
