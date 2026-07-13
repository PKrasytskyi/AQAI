package ua.demo.agentlab.artifactreuse.metrics;

public record ArtifactReuseTypeMetrics(int hits, int misses) {
    public ArtifactReuseTypeMetrics {
        hits = Math.max(0, hits);
        misses = Math.max(0, misses);
    }
}
