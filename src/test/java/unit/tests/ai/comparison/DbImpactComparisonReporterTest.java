package unit.tests.ai.comparison;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.comparison.DbImpactComparisonReport;
import ua.demo.agentlab.ai.comparison.DbImpactComparisonReporter;

import java.nio.file.Files;
import java.nio.file.Path;

public class DbImpactComparisonReporterTest {

    @Test
    public void reporterBuildsPresentationRowsFromTwoRunSnapshots() throws Exception {
        Path temp = Files.createTempDirectory("db-impact-comparison");
        Path withoutDb = temp.resolve("without-db");
        Path withDb = temp.resolve("with-db");
        writeRun(withoutDb, "without-db", 78, 5, 4, 3, "FAILED", 2,
                false, false, false, "disabled", false);
        writeRun(withDb, "with-db", 88, 2, 1, 1, "PASSED", 3,
                true, true, true, "stable-page-cache", true);

        Path output = temp.resolve("comparison-output");
        DbImpactComparisonReport report = new DbImpactComparisonReporter().compare(withoutDb, withDb, output);

        Assert.assertEquals(report.withoutDbRun(), "without-db");
        Assert.assertEquals(report.withDbRun(), "with-db");
        Assert.assertTrue(report.rows().stream()
                .anyMatch(row -> row.metric().equals("Quality score")
                        && row.withoutDb().equals("78/100")
                        && row.withDb().equals("88/100")
                        && row.change().equals("+10")));
        Assert.assertTrue(report.rows().stream()
                .anyMatch(row -> row.metric().equals("DB mode")
                        && row.withoutDb().equals("without-db")
                        && row.withDb().equals("with-db")));
        Assert.assertTrue(Files.exists(output.resolve("db-impact-summary.json")));
        Assert.assertTrue(Files.readString(output.resolve("db-impact-summary.md"))
                .contains("| Quality score | 78/100 | 88/100 | +10 |"));
    }

    @Test
    public void reporterOrdersRunsByDbReadinessWhenArgumentsAreReversed() throws Exception {
        Path temp = Files.createTempDirectory("db-impact-comparison-order");
        Path withoutDb = temp.resolve("without-db");
        Path withDb = temp.resolve("with-db");
        writeRun(withoutDb, "without-db", 70, 4, 3, 2, "FAILED", 1,
                false, false, false, "disabled", false);
        writeRun(withDb, "with-db", 90, 1, 0, 0, "PASSED", 2,
                true, true, true, "stable-page-cache", true);

        DbImpactComparisonReport report = new DbImpactComparisonReporter()
                .compare(withDb, withoutDb, temp.resolve("comparison-output"));

        Assert.assertEquals(report.withoutDbRun(), "without-db");
        Assert.assertEquals(report.withDbRun(), "with-db");
        Assert.assertEquals(report.withoutDb().dbUsageMode(), "without-db");
        Assert.assertEquals(report.withDb().dbUsageMode(), "with-db");
    }

    @Test
    public void reporterRejectsPartialDbComparison() throws Exception {
        Path temp = Files.createTempDirectory("db-impact-comparison-partial");
        Path withoutDb = temp.resolve("without-db");
        Path partialDb = temp.resolve("partial-db");
        writeRun(withoutDb, "without-db", 70, 4, 3, 2, "FAILED", 1,
                false, false, false, "disabled", false);
        writeRun(partialDb, "partial-db", 75, 3, 2, 1, "PASSED", 2,
                true, false, true, "stable-page-cache", true);

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class,
                () -> new DbImpactComparisonReporter().compare(withoutDb, partialDb, temp.resolve("comparison-output")));

        Assert.assertTrue(exception.getMessage().contains("requires one clean DB-disabled run"));
    }

    @Test
    public void reporterCountsFailedEnrichmentAttemptAsLlmCall() throws Exception {
        Path temp = Files.createTempDirectory("db-impact-comparison-llm");
        Path withoutDb = temp.resolve("without-db");
        Path withDb = temp.resolve("with-db");
        writeRun(withoutDb, "without-db", 82, 0, 0, 0, "PASSED", 2,
                false, false, false, "current-run", false);
        writeRun(withDb, "with-db", 82, 0, 0, 0, "PASSED", 2,
                true, true, true, "stable-page-cache", true);
        Files.writeString(withoutDb.resolve("ai-run").resolve("page-object-spec").resolve("DashboardPage-prompt.txt"),
                "# Role\nDashboard prompt\n");
        Files.writeString(withDb.resolve("ai-run").resolve("page-object-spec").resolve("DashboardPage-prompt.txt"),
                "# Role\nDashboard prompt\n");
        Files.writeString(withoutDb.resolve("ai-run").resolve("enrichment").resolve("page-model-enrichment-report.json"), """
                {"records":2,"openAiRecords":1,"failures":["login: parse failed"]}
                """);
        Files.writeString(withDb.resolve("ai-run").resolve("enrichment").resolve("page-model-enrichment-report.json"), """
                {"records":2,"cacheHits":2,"openAiRecords":0,"failures":[]}
                """);

        DbImpactComparisonReport report = new DbImpactComparisonReporter()
                .compare(withoutDb, withDb, temp.resolve("comparison-output"));

        Assert.assertEquals(report.withoutDb().pomLlmCalls(), 2);
        Assert.assertEquals(report.withoutDb().pageEnrichmentOpenAiCalls(), 2);
        Assert.assertEquals(report.withoutDb().pageEnrichmentOpenAiSuccesses(), 1);
        Assert.assertEquals(report.withoutDb().pageEnrichmentOpenAiFailures(), 1);
        Assert.assertEquals(report.withoutDb().llmCalls(), 4);
        Assert.assertEquals(report.withDb().pageEnrichmentOpenAiCalls(), 0);
    }

    @Test
    public void reporterShowsMissingCompileArtifactInsteadOfZeroRatio() throws Exception {
        Path temp = Files.createTempDirectory("db-impact-comparison-missing-compile");
        Path withoutDb = temp.resolve("without-db");
        Path withDb = temp.resolve("with-db");
        writeRun(withoutDb, "without-db", 82, 0, 0, 0, "PASSED", 2,
                false, false, false, "current-run", false);
        writeRun(withDb, "with-db", 82, 0, 0, 0, "PASSED", 2,
                true, true, true, "stable-page-cache", true);
        Files.delete(withDb.resolve("ai-run").resolve("validation").resolve("generated-ui-smoke-result.json"));

        DbImpactComparisonReport report = new DbImpactComparisonReporter()
                .compare(withoutDb, withDb, temp.resolve("comparison-output"));

        Assert.assertEquals(report.withDb().compileStatus(), "missing-artifact");
        Assert.assertTrue(report.rows().stream()
                .anyMatch(row -> row.metric().equals("Compile-ready generated code")
                        && row.withDb().equals("missing-artifact")));
    }

    @Test
    public void reporterUsesKnownLlmTokenUsageArtifactsBeforeTokenizerEstimate() throws Exception {
        Path temp = Files.createTempDirectory("db-impact-comparison-token-usage");
        Path withoutDb = temp.resolve("without-db");
        Path withDb = temp.resolve("with-db");
        writeRun(withoutDb, "without-db", 82, 0, 0, 0, "PASSED", 2,
                false, false, false, "current-run", false);
        writeRun(withDb, "with-db", 82, 0, 0, 0, "PASSED", 2,
                true, true, true, "stable-page-cache", true);
        Files.createDirectories(withDb.resolve("ai-run").resolve("page-objects"));
        Files.writeString(withDb.resolve("ai-run").resolve("page-objects").resolve("pom-llm-token-usage.json"), """
                {"inputTokens":70,"outputTokens":30,"totalTokens":100}
                """);
        Files.writeString(withDb.resolve("ai-run").resolve("enrichment").resolve("page-model-enrichment-report.json"), """
                {"inputTokens":15,"outputTokens":5,"totalTokens":20,"openAiAttempts":1,"openAiSuccesses":1}
                """);
        Files.writeString(withDb.resolve("ai-run").resolve("quality").resolve("noise.json"), """
                {"totalTokens":99999}
                """);

        DbImpactComparisonReport report = new DbImpactComparisonReporter()
                .compare(withoutDb, withDb, temp.resolve("comparison-output"));

        Assert.assertEquals(report.withDb().totalTokens(), 120);
        Assert.assertEquals(report.withDb().promptTokens(), 85);
        Assert.assertEquals(report.withDb().responseTokens(), 35);
        Assert.assertEquals(report.withDb().actualTokens(), 120);
        Assert.assertFalse(report.withDb().tokenUsageEstimated());
        Assert.assertEquals(report.withDb().tokenCountingMode(), "openai-usage");
    }

    @Test
    public void reporterUsesExplicitArtifactReuseMetricsInsteadOfPromptFileCount() throws Exception {
        Path temp = Files.createTempDirectory("db-impact-comparison-artifact-reuse");
        Path withoutDb = temp.resolve("without-db");
        Path withDb = temp.resolve("with-db");
        writeRun(withoutDb, "without-db", 80, 0, 0, 0, "PASSED", 2,
                false, false, false, "current-run", false);
        writeRun(withDb, "with-db", 88, 0, 0, 0, "PASSED", 2,
                true, true, true, "stable-page-cache", true);
        writeArtifactReuseMetrics(withoutDb, true, 2, 0, 0, 2, 0, 0, 0, 0);
        writeArtifactReuseMetrics(withDb, true, 0, 2, 2, 0, 3826, 2, 0, 4);

        DbImpactComparisonReport report = new DbImpactComparisonReporter()
                .compare(withoutDb, withDb, temp.resolve("comparison-output"));

        Assert.assertEquals(report.withoutDb().pomLlmCalls(), 2);
        Assert.assertEquals(report.withDb().pomLlmCalls(), 0);
        Assert.assertEquals(report.withDb().artifactReuseLlmCallsSkipped(), 2);
        Assert.assertEquals(report.withDb().artifactCacheHits(), 2);
        Assert.assertEquals(report.withDb().artifactTokensSavedEstimate(), 3826);
        Assert.assertEquals(report.withDb().stableLocatorReuse(), 4);
        Assert.assertTrue(report.rows().stream().anyMatch(row -> row.metric().equals("POM LLM calls skipped")
                && row.withDb().equals("2")));
    }

    @Test
    public void reporterCountsLegacyReusedContractFromStableArtifactDecision() throws Exception {
        Path temp = Files.createTempDirectory("db-impact-comparison-reused-contract");
        Path withoutDb = temp.resolve("without-db");
        Path withDb = temp.resolve("with-db");
        writeRun(withoutDb, "without-db", 80, 0, 0, 0, "PASSED", 1,
                false, false, false, "disabled", false);
        writeRun(withDb, "with-db", 90, 0, 0, 0, "PASSED", 1,
                true, true, true, "stable-page-cache", true);

        Path pageObjects = withDb.resolve("ai-run").resolve("page-object-spec");
        Path stableContract = temp.resolve("stable").resolve("LoginPage.fingerprint.pom-contract.json");
        Files.createDirectories(stableContract.getParent());
        String contract = Files.readString(pageObjects.resolve("LoginPage-pom-contract.json"));
        Files.writeString(stableContract, contract);
        Files.delete(pageObjects.resolve("LoginPage-pom-contract.json"));
        Files.writeString(pageObjects.resolve("LoginPage-artifact-reuse-decision.json"), """
                {"decision":"REUSE_STABLE","stableFilePath":"%s"}
                """.formatted(stableContract.toAbsolutePath().normalize().toString().replace("\\", "\\\\")));

        DbImpactComparisonReport report = new DbImpactComparisonReporter()
                .compare(withoutDb, withDb, temp.resolve("comparison-output"));

        Assert.assertEquals(report.withDb().newlyTotalPomContracts(), 0);
        Assert.assertEquals(report.withDb().reusedValidPomContracts(), 1);
        Assert.assertEquals(report.withDb().validPomContracts(), 1);
        Assert.assertTrue(report.rows().stream().anyMatch(row -> row.metric().equals("Effective valid POM contracts")
                && row.withDb().equals("1/1")));
    }

    private void writeRun(
            Path root,
            String runId,
            int qualityScore,
            int lowConfidence,
            int expectedReview,
            int promptBlocks,
            String smokeStatus,
            int filesChecked,
            boolean neo4jHit,
            boolean qdrantHit,
            boolean stableCacheUsed,
            String retrievalMode,
            boolean cacheHit
    ) throws Exception {
        Path aiRun = root.resolve("ai-run");
        Files.createDirectories(aiRun.resolve("quality"));
        Files.createDirectories(aiRun.resolve("page-object-spec"));
        Files.createDirectories(aiRun.resolve("validation"));
        Files.createDirectories(aiRun.resolve("enrichment"));
        Files.writeString(aiRun.resolve("quality").resolve("run-quality-summary.json"), """
                {
                  "runId": "%s",
                  "qualityScore": %d,
                  "lowConfidenceLocators": %d,
                  "expectedResultsNeedsReview": %d,
                  "promptBlockingIssues": %d,
                  "neo4jHit": %s,
                  "qdrantHit": %s,
                  "stableCacheUsed": %s,
                  "retrievalMode": "%s"
                }
                """.formatted(runId, qualityScore, lowConfidence, expectedReview, promptBlocks,
                neo4jHit, qdrantHit, stableCacheUsed, retrievalMode));
        Files.writeString(aiRun.resolve("validation").resolve("generated-ui-smoke-result.json"), """
                {"status":"%s","filesChecked":%d,"issues":[]}
                """.formatted(smokeStatus, filesChecked));
        Files.writeString(aiRun.resolve("enrichment").resolve("page-knowledge-cache-lookup.json"), """
                {"entries":[{"hit":%s}]}
                """.formatted(cacheHit));
        Files.writeString(aiRun.resolve("page-object-spec").resolve("LoginPage-prompt.txt"), """
                # Role
                You are a Page Object Contract Planner.
                # Input
                This prompt is intentionally long enough to be counted as a repeated fragment candidate.
                """);
        Files.writeString(aiRun.resolve("page-object-spec").resolve("LoginPage-pom-contract.json"), """
                {
                  "schemaVersion":"pom-contract-v1",
                  "page":{"name":"LoginPage","route":"/login","capability":"AUTHENTICATION","openMethod":"openLogin"},
                  "locators":[{"id":"usernameInput","elementName":"username","strategy":"name","value":"username","role":"input","stabilityScore":0.9}],
                  "components":[],
                  "actions":[],
                  "assertions":[{"methodName":"isUsernameVisible","returnType":"boolean","checks":[{"check":"VISIBLE","locator":"usernameInput","expectedValue":"","valueFrom":"","attribute":"","route":""}],"combine":"AND"}],
                  "coverageGaps":[],
                  "rejectedSuggestions":[]
                }
                """);
    }

    private void writeArtifactReuseMetrics(
            Path root,
            boolean enabled,
            int executed,
            int skipped,
            int hits,
            int misses,
            int tokensSaved,
            int stable,
            int needsReview,
            int stableLocatorReuse
    ) throws Exception {
        Path metrics = root.resolve("ai-run").resolve("metrics");
        Files.createDirectories(metrics);
        Files.writeString(metrics.resolve("artifact-reuse-summary.json"), """
                {
                  "schemaVersion":"artifact-reuse-run-metrics.v1",
                  "enabled":%s,
                  "llmCallsExecuted":%d,
                  "llmCallsSkipped":%d,
                  "artifactCacheHits":%d,
                  "artifactCacheMisses":%d,
                  "tokensSavedEstimate":%d,
                  "stableArtifacts":%d,
                  "needsReviewArtifacts":%d,
                  "stableLocatorReuse":%d,
                  "flowReuse":0
                }
                """.formatted(enabled, executed, skipped, hits, misses, tokensSaved, stable, needsReview,
                stableLocatorReuse));
    }
}
