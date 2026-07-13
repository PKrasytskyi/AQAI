package ua.demo.agentlab.artifactreuse.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArtifactReuseRunMetricsWriter {

    private final Path runRoot;
    private final ObjectMapper objectMapper;

    public ArtifactReuseRunMetricsWriter() {
        this(Path.of("target", "ai-run"));
    }

    public ArtifactReuseRunMetricsWriter(Path runRoot) {
        this.runRoot = runRoot == null ? Path.of("target", "ai-run") : runRoot;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ArtifactReuseMetricsWriteResult write(ArtifactReuseRunMetrics metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("metrics cannot be null");
        }
        try {
            Path metricsDirectory = runRoot.resolve("metrics");
            Files.createDirectories(metricsDirectory);
            Path metricsFile = metricsDirectory.resolve("artifact-reuse-summary.json");
            objectMapper.writeValue(metricsFile.toFile(), metrics);
            Path summaryFile = updateRunSummary(metrics);
            Path markdownFile = updateRunSummaryMarkdown(metrics);
            return ArtifactReuseMetricsWriteResult.success(
                    metricsFile.toAbsolutePath().normalize(),
                    summaryFile.toAbsolutePath().normalize(),
                    markdownFile.toAbsolutePath().normalize()
            );
        } catch (IOException exception) {
            return ArtifactReuseMetricsWriteResult.failed("Failed to write artifact reuse metrics: " + exception.getMessage());
        }
    }

    private Path updateRunSummary(ArtifactReuseRunMetrics metrics) throws IOException {
        Path summaryFile = runRoot.resolve("run-summary.json");
        ObjectNode summary = readObject(summaryFile);
        summary.set("artifactReuse", objectMapper.valueToTree(metrics));
        objectMapper.writeValue(summaryFile.toFile(), summary);
        return summaryFile;
    }

    private Path updateRunSummaryMarkdown(ArtifactReuseRunMetrics metrics) throws IOException {
        Path markdownFile = runRoot.resolve("run-summary.md");
        String existing = Files.isRegularFile(markdownFile)
                ? Files.readString(markdownFile, StandardCharsets.UTF_8).trim()
                : "# AI Run Summary";
        int marker = existing.indexOf("## Artifact Reuse");
        if (marker >= 0) {
            existing = existing.substring(0, marker).trim();
        }
        String section = """

                ## Artifact Reuse

                | Metric | Value |
                | --- | --- |
                | enabled | %s |
                | llmCallsExecuted | %d |
                | llmCallsSkipped | %d |
                | artifactCacheHits | %d |
                | artifactCacheMisses | %d |
                | tokensSavedEstimate | %d |
                | stableArtifacts | %d |
                | needsReviewArtifacts | %d |
                | stableLocatorReuse | %d |
                | flowReuse | %d |
                """.formatted(
                metrics.enabled(),
                metrics.llmCallsExecuted(),
                metrics.llmCallsSkipped(),
                metrics.artifactCacheHits(),
                metrics.artifactCacheMisses(),
                metrics.tokensSavedEstimate(),
                metrics.stableArtifacts(),
                metrics.needsReviewArtifacts(),
                metrics.stableLocatorReuse(),
                metrics.flowReuse()
        );
        Files.writeString(markdownFile, existing + section, StandardCharsets.UTF_8);
        return markdownFile;
    }

    private ObjectNode readObject(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return objectMapper.createObjectNode();
        }
        JsonNode node = objectMapper.readTree(path.toFile());
        return node instanceof ObjectNode objectNode ? objectNode : objectMapper.createObjectNode();
    }
}
