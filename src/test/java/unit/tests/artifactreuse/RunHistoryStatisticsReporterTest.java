package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.metrics.RunHistoryStatisticsReporter;

import java.nio.file.Files;
import java.nio.file.Path;

public class RunHistoryStatisticsReporterTest {

    @Test
    public void writesOneTableWithCurrentAndArchivedRunFinalStates() throws Exception {
        Path root = Files.createTempDirectory("run-history-statistics");
        Path history = root.resolve("history");
        Path archived = history.resolve("run-one").resolve("ai-run");
        Path current = root.resolve("ai-run");
        writeRun(archived, "run-one", true, 1, "PASSED", "PASSED", "PASSED", true);
        writeRun(current, "run-two", true, 2, "PASSED", "PASSED", "PASSED", true);

        var report = new RunHistoryStatisticsReporter(history, current).writeLatestRuns(10);
        String markdown = Files.readString(report.markdownFile());

        Assert.assertEquals(report.runs().size(), 2);
        Assert.assertTrue(markdown.contains("| run-two |"));
        Assert.assertTrue(markdown.contains("stable-page-cache (neo4j=true, qdrant=true, stableCache=true)"));
        Assert.assertTrue(markdown.contains("| PASSED |"));
        Assert.assertTrue(Files.isRegularFile(history.resolve("last-10-runs.md")));
    }

    private void writeRun(
            Path run,
            String runId,
            boolean databaseUsed,
            int pomHits,
            String compile,
            String generatedSmoke,
            String liveSmoke,
            boolean feedbackSuccess
    ) throws Exception {
        Files.createDirectories(run.resolve("validation"));
        Files.createDirectories(run.resolve("flow-contracts"));
        Files.writeString(run.resolve("run-summary.json"), """
                {"runId":"%s","requirements":4,"canonicalTestCases":2,"qualityScore":92,
                 "neo4jHit":%s,"qdrantHit":%s,"stableCacheUsed":%s,"retrievalMode":"stable-page-cache",
                 "artifactReuse":{"llmCallsExecuted":0,"llmCallsSkipped":2,
                 "reuseByArtifactType":{"POM_CONTRACT":{"hits":%d,"misses":0},"FLOW_CONTRACT":{"hits":3,"misses":1}}}}
                """.formatted(runId, databaseUsed, databaseUsed, databaseUsed, pomHits));
        Files.writeString(run.resolve("validation").resolve("artifact-lifecycle-result.json"),
                "{\"stableArtifacts\":2,\"needsReviewArtifacts\":0}");
        Files.writeString(run.resolve("validation").resolve("generated-code-compile-result.json"),
                "{\"status\":\"%s\"}".formatted(compile));
        Files.writeString(run.resolve("validation").resolve("generated-code-review-result.json"),
                "{\"totalFindings\":0}");
        Files.writeString(run.resolve("validation").resolve("generated-ui-smoke-result.json"),
                "{\"status\":\"%s\"}".formatted(generatedSmoke));
        Files.writeString(run.resolve("validation").resolve("live-ui-smoke-result.json"),
                "{\"status\":\"%s\"}".formatted(liveSmoke));
        Files.writeString(run.resolve("flow-contracts").resolve("flow-runtime-feedback.json"),
                "{\"success\":%s}".formatted(feedbackSuccess));
    }
}
