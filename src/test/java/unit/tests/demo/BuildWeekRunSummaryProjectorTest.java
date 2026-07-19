package unit.tests.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.demo.BuildWeekDemoSummary;
import ua.demo.agentlab.demo.BuildWeekRunSummaryProjector;

import java.nio.file.Files;
import java.util.List;

public class BuildWeekRunSummaryProjectorTest {

    @Test
    public void terminalFailureCapsQualityAndRemainsVisibleInPrimarySummary() throws Exception {
        var directory = Files.createTempDirectory("build-week-summary");
        var summaryFile = directory.resolve("run-summary.json");
        Files.writeString(summaryFile, "{\"qualityScore\":99}");
        BuildWeekDemoSummary summary = new BuildWeekDemoSummary(
                BuildWeekDemoSummary.SCHEMA_VERSION,
                "demo", "run", "FAILED", "HYBRID-REUSE", "OrangeHRM", "gpt-5.6-luna",
                "PARTIAL_REUSE", 1, 1, 0, 2, 1, 0, 2,
                true, true, true, 0, 4, 2, "VERIFIED", 85, 0, 1, 2, 2, 4, 4,
                "PASSED", 0, "PASSED", "PASSED", "FAILED",
                4, 3, 1, "Neo4j feedback updated",
                List.of("POM planning"), List.of("Java generation"), List.of("one test failed")
        );

        new BuildWeekRunSummaryProjector(summaryFile).project(summary);

        var json = new ObjectMapper().readTree(summaryFile.toFile());
        Assert.assertEquals(json.path("qualityScore").asInt(), 85);
        Assert.assertEquals(json.path("terminalFailure").asText(), "BUILD_WEEK_DEMO_ACCEPTANCE");
        Assert.assertEquals(json.path("buildWeekDemo").path("generatedTestExecutionStatus").asText(), "FAILED");
    }
}
