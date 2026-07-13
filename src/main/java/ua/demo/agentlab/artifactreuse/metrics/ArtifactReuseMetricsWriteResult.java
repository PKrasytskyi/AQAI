package ua.demo.agentlab.artifactreuse.metrics;

import java.nio.file.Path;

public record ArtifactReuseMetricsWriteResult(
        boolean success,
        Path metricsFile,
        Path runSummaryFile,
        Path runSummaryMarkdownFile,
        String message
) {
    public static ArtifactReuseMetricsWriteResult success(Path metricsFile, Path runSummaryFile, Path markdownFile) {
        return new ArtifactReuseMetricsWriteResult(true, metricsFile, runSummaryFile, markdownFile, "artifact reuse metrics written");
    }

    public static ArtifactReuseMetricsWriteResult failed(String message) {
        return new ArtifactReuseMetricsWriteResult(false, null, null, null, message == null ? "" : message.trim());
    }
}
