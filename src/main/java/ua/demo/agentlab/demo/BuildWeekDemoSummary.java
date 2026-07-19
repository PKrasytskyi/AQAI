package ua.demo.agentlab.demo;

import java.util.List;

public record BuildWeekDemoSummary(
        String schemaVersion,
        String demoId,
        String runId,
        String status,
        String runMode,
        String projectName,
        String openAiModel,
        String aiReasoningStatus,
        int pageEnrichmentLlmCalls,
        int pageEnrichmentCacheHits,
        int pageEnrichmentLlmFailures,
        int pomContractLlmCalls,
        int stablePomContractsReused,
        int pomContractLlmCallsSkipped,
        int avoidedLlmCalls,
        boolean neo4jHit,
        boolean qdrantHit,
        boolean stableEvidenceReused,
        int testGenerationLlmCalls,
        int canonicalScenarios,
        int confirmedCatalogPages,
        String locatorEvidenceStatus,
        int qualityScore,
        int coverageGapCount,
        int blockingIssueCount,
        int pomContracts,
        int generatedPageObjects,
        int uiTestContracts,
        int generatedTests,
        String compileStatus,
        int reviewFindings,
        String generatedSourceSmokeStatus,
        String liveSmokeStatus,
        String generatedTestExecutionStatus,
        int executedTests,
        int passedTests,
        int failedTests,
        String runtimeFeedbackDbStatus,
        List<String> aiResponsibilities,
        List<String> deterministicResponsibilities,
        List<String> issues
) {
    public static final String SCHEMA_VERSION = "build-week-demo-summary.v2";

    public BuildWeekDemoSummary {
        schemaVersion = text(schemaVersion).isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        demoId = text(demoId);
        runId = text(runId);
        status = text(status);
        runMode = text(runMode);
        projectName = text(projectName);
        openAiModel = text(openAiModel);
        aiReasoningStatus = text(aiReasoningStatus);
        pageEnrichmentLlmCalls = nonNegative(pageEnrichmentLlmCalls);
        pageEnrichmentCacheHits = nonNegative(pageEnrichmentCacheHits);
        pageEnrichmentLlmFailures = nonNegative(pageEnrichmentLlmFailures);
        pomContractLlmCalls = nonNegative(pomContractLlmCalls);
        stablePomContractsReused = nonNegative(stablePomContractsReused);
        pomContractLlmCallsSkipped = nonNegative(pomContractLlmCallsSkipped);
        avoidedLlmCalls = nonNegative(avoidedLlmCalls);
        testGenerationLlmCalls = nonNegative(testGenerationLlmCalls);
        canonicalScenarios = nonNegative(canonicalScenarios);
        confirmedCatalogPages = nonNegative(confirmedCatalogPages);
        locatorEvidenceStatus = text(locatorEvidenceStatus);
        qualityScore = Math.min(100, nonNegative(qualityScore));
        coverageGapCount = nonNegative(coverageGapCount);
        blockingIssueCount = nonNegative(blockingIssueCount);
        pomContracts = nonNegative(pomContracts);
        generatedPageObjects = nonNegative(generatedPageObjects);
        uiTestContracts = nonNegative(uiTestContracts);
        generatedTests = nonNegative(generatedTests);
        compileStatus = text(compileStatus);
        reviewFindings = nonNegative(reviewFindings);
        generatedSourceSmokeStatus = text(generatedSourceSmokeStatus);
        liveSmokeStatus = text(liveSmokeStatus);
        generatedTestExecutionStatus = text(generatedTestExecutionStatus);
        executedTests = nonNegative(executedTests);
        passedTests = nonNegative(passedTests);
        failedTests = nonNegative(failedTests);
        runtimeFeedbackDbStatus = text(runtimeFeedbackDbStatus);
        aiResponsibilities = copy(aiResponsibilities);
        deterministicResponsibilities = copy(deterministicResponsibilities);
        issues = copy(issues);
    }

    public boolean passed() {
        return "PASSED".equals(status) && issues.isEmpty();
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
