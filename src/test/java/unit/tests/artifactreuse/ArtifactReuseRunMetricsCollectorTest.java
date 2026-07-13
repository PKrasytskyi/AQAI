package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseRunMetrics;
import ua.demo.agentlab.artifactreuse.metrics.ArtifactReuseRunMetricsCollector;

import java.util.Map;

public class ArtifactReuseRunMetricsCollectorTest {

    @Test
    public void collectsExplicitArtifactReuseMetricsFromRunArtifacts() {
        ArtifactReuseRunMetrics metrics = new ArtifactReuseRunMetricsCollector().collect(Map.ofEntries(
                Map.entry("artifact.reuse.enabled", "true"),
                Map.entry("artifact.reuse.forceRefresh", "false"),
                Map.entry("pom.contract.llm.attempt.count", "1"),
                Map.entry("artifact.reuse.llm.skipped.count", "2"),
                Map.entry("artifact.reuse.hit.count", "2"),
                Map.entry("artifact.reuse.miss.count", "1"),
                Map.entry("artifact.reuse.tokens.saved.estimate", "3826"),
                Map.entry("artifact.lifecycle.stable.count", "2"),
                Map.entry("artifact.lifecycle.needs-review.count", "1"),
                Map.entry("ai.context.db.stable.locator.count", "4"),
                Map.entry("artifact.reuse.flow.stable.count", "3"),
                Map.entry("artifact.reuse.flow.discover.count", "1")
        ));

        Assert.assertTrue(metrics.enabled());
        Assert.assertEquals(metrics.llmCallsExecuted(), 1);
        Assert.assertEquals(metrics.llmCallsSkipped(), 2);
        Assert.assertEquals(metrics.artifactCacheHits(), 2);
        Assert.assertEquals(metrics.tokensSavedEstimate(), 3826);
        Assert.assertEquals(metrics.stableLocatorReuse(), 4);
        Assert.assertEquals(metrics.reuseByArtifactType().get("POM_CONTRACT").hits(), 2);
        Assert.assertEquals(metrics.flowReuse(), 3);
        Assert.assertEquals(metrics.reuseByArtifactType().get("FLOW_CONTRACT").misses(), 1);
    }
}
