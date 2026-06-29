package ua.demo.agentlab.ai.quality;

public record AiRunQualitySummary(
        String runId,
        int requirements,
        int canonicalTestCases,
        int expectedResultsResolved,
        int expectedResultsNeedsReview,
        int mappedPages,
        int routeCollisions,
        int externalEvidenceRejected,
        int lowConfidenceLocators,
        int promptBlockingIssues,
        double averageLocatorScore,
        int qualityScore
) {
    public AiRunQualitySummary {
        runId = runId == null || runId.isBlank() ? "unknown-run" : runId.trim();
        requirements = Math.max(0, requirements);
        canonicalTestCases = Math.max(0, canonicalTestCases);
        expectedResultsResolved = Math.max(0, expectedResultsResolved);
        expectedResultsNeedsReview = Math.max(0, expectedResultsNeedsReview);
        mappedPages = Math.max(0, mappedPages);
        routeCollisions = Math.max(0, routeCollisions);
        externalEvidenceRejected = Math.max(0, externalEvidenceRejected);
        lowConfidenceLocators = Math.max(0, lowConfidenceLocators);
        promptBlockingIssues = Math.max(0, promptBlockingIssues);
        averageLocatorScore = Math.max(0.0d, Math.min(1.0d, averageLocatorScore));
        qualityScore = Math.max(0, Math.min(100, qualityScore));
    }
}
