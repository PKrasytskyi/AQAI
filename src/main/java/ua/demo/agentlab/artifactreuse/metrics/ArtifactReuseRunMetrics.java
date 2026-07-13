package ua.demo.agentlab.artifactreuse.metrics;

import java.util.Map;

public record ArtifactReuseRunMetrics(
        String schemaVersion,
        boolean enabled,
        boolean forceRefresh,
        int llmCallsExecuted,
        int llmCallsSkipped,
        int artifactCacheHits,
        int artifactCacheMisses,
        int tokensSavedEstimate,
        int stableArtifacts,
        int needsReviewArtifacts,
        int stableLocatorReuse,
        int flowReuse,
        Map<String, ArtifactReuseTypeMetrics> reuseByArtifactType
) {
    public ArtifactReuseRunMetrics {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? "artifact-reuse-run-metrics.v1"
                : schemaVersion.trim();
        llmCallsExecuted = Math.max(0, llmCallsExecuted);
        llmCallsSkipped = Math.max(0, llmCallsSkipped);
        artifactCacheHits = Math.max(0, artifactCacheHits);
        artifactCacheMisses = Math.max(0, artifactCacheMisses);
        tokensSavedEstimate = Math.max(0, tokensSavedEstimate);
        stableArtifacts = Math.max(0, stableArtifacts);
        needsReviewArtifacts = Math.max(0, needsReviewArtifacts);
        stableLocatorReuse = Math.max(0, stableLocatorReuse);
        flowReuse = Math.max(0, flowReuse);
        reuseByArtifactType = reuseByArtifactType == null ? Map.of() : Map.copyOf(reuseByArtifactType);
    }
}
