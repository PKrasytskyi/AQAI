package unit.tests.artifactreuse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseRunMetrics;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseRunMetricsWriter;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseTypeMetrics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ArtifactReuseRunMetricsWriterTest {

    @Test
    public void writesDedicatedMetricsAndUpdatesCompactRunSummary() throws Exception {
        Path root = Files.createTempDirectory("artifact-reuse-metrics");
        Files.writeString(root.resolve("run-summary.json"), "{\"runId\":\"run-1\"}");
        Files.writeString(root.resolve("run-summary.md"), "# AI Run Summary\n");
        ArtifactReuseRunMetrics metrics = new ArtifactReuseRunMetrics(
                "artifact-reuse-run-metrics.v1", true, false,
                0, 2, 2, 0, 3826, 2, 0, 4, 0,
                Map.of("POM_CONTRACT", new ArtifactReuseTypeMetrics(2, 0))
        );

        var result = new ArtifactReuseRunMetricsWriter(root).write(metrics);
        var summary = new ObjectMapper().readTree(root.resolve("run-summary.json").toFile());

        Assert.assertTrue(result.success());
        Assert.assertTrue(Files.isRegularFile(root.resolve("metrics").resolve("artifact-reuse-summary.json")));
        Assert.assertEquals(summary.path("runId").asText(), "run-1");
        Assert.assertEquals(summary.path("artifactReuse").path("llmCallsSkipped").asInt(), 2);
        Assert.assertTrue(Files.readString(root.resolve("run-summary.md")).contains("## Artifact Reuse"));
    }
}
