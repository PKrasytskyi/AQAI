package ua.demo.agentlab.artifactreuse.metrics;

import java.util.Map;

public class ArtifactReuseRunMetricsCollector {

    public ArtifactReuseRunMetrics collect(Map<String, String> artifacts) {
        Map<String, String> values = artifacts == null ? Map.of() : artifacts;
        int hits = integer(values, "artifact.reuse.hit.count");
        int misses = integer(values, "artifact.reuse.miss.count");
        return new ArtifactReuseRunMetrics(
                "artifact-reuse-run-metrics.v1",
                bool(values, "artifact.reuse.enabled"),
                bool(values, "artifact.reuse.forceRefresh"),
                integer(values, "pom.contract.llm.attempt.count"),
                integer(values, "artifact.reuse.llm.skipped.count"),
                hits,
                misses,
                integer(values, "artifact.reuse.tokens.saved.estimate"),
                integer(values, "artifact.lifecycle.stable.count"),
                integer(values, "artifact.lifecycle.needs-review.count"),
                integer(values, "ai.context.db.stable.locator.count"),
                integer(values, "artifact.reuse.flow.stable.count"),
                Map.of(
                        "POM_CONTRACT", new ArtifactReuseTypeMetrics(hits, misses),
                        "FLOW_CONTRACT", new ArtifactReuseTypeMetrics(integer(values, "artifact.reuse.flow.stable.count"),
                                integer(values, "artifact.reuse.flow.discover.count"))
                )
        );
    }

    private int integer(Map<String, String> artifacts, String key) {
        try {
            return Math.max(0, Integer.parseInt(artifacts.getOrDefault(key, "0").trim()));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean bool(Map<String, String> artifacts, String key) {
        return Boolean.parseBoolean(artifacts.getOrDefault(key, "false").trim());
    }
}
