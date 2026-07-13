package ua.demo.agentlab.artifactreuse.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Builds one human-readable table from final artifacts of the latest completed AI runs. */
public class RunHistoryStatisticsReporter {

    public static final int DEFAULT_RUN_LIMIT = 10;
    private final Path historyRoot;
    private final Path currentRunRoot;
    private final ObjectMapper objectMapper;

    public RunHistoryStatisticsReporter() {
        this(Path.of("target", "ai-run-history"), Path.of("target", "ai-run"));
    }

    public RunHistoryStatisticsReporter(Path historyRoot, Path currentRunRoot) {
        this.historyRoot = historyRoot == null ? Path.of("target", "ai-run-history") : historyRoot;
        this.currentRunRoot = currentRunRoot == null ? Path.of("target", "ai-run") : currentRunRoot;
        this.objectMapper = new ObjectMapper();
    }

    public RunHistoryStatisticsReport writeLatestRuns() {
        return writeLatestRuns(DEFAULT_RUN_LIMIT);
    }

    public RunHistoryStatisticsReport writeLatestRuns(int limit) {
        int normalizedLimit = Math.max(1, limit);
        List<Path> roots = latestRunRoots(normalizedLimit);
        List<RunHistoryStatisticsRow> rows = roots.stream().map(this::readRow).toList();
        Path report = historyRoot.resolve("last-" + normalizedLimit + "-runs.md");
        try {
            Files.createDirectories(historyRoot);
            Files.writeString(report, markdown(rows, normalizedLimit), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write run history statistics: " + exception.getMessage(), exception);
        }
        return new RunHistoryStatisticsReport(normalizedLimit, rows, report.toAbsolutePath().normalize());
    }

    private List<Path> latestRunRoots(int limit) {
        Map<String, Path> uniqueByRunId = new LinkedHashMap<>();
        List<Path> candidates = new ArrayList<>();
        if (Files.isDirectory(historyRoot)) {
            try (Stream<Path> paths = Files.list(historyRoot)) {
                paths.filter(Files::isDirectory)
                        .map(this::aiRunRoot)
                        .filter(this::hasRunSummary)
                        .forEach(candidates::add);
            } catch (IOException ignored) {
                // A partial history must not break the completed workflow report.
            }
        }
        if (hasRunSummary(currentRunRoot)) {
            candidates.add(currentRunRoot);
        }
        candidates.stream()
                .sorted(Comparator.comparingLong(this::lastModified).reversed())
                .forEach(root -> uniqueByRunId.putIfAbsent(runId(root), root));
        return uniqueByRunId.values().stream().limit(limit).toList();
    }

    private RunHistoryStatisticsRow readRow(Path root) {
        JsonNode summary = read(root.resolve("run-summary.json"));
        JsonNode reuse = summary.path("artifactReuse");
        JsonNode reuseTypes = reuse.path("reuseByArtifactType");
        JsonNode lifecycle = read(root.resolve("validation").resolve("artifact-lifecycle-result.json"));
        JsonNode compile = read(root.resolve("validation").resolve("generated-code-compile-result.json"));
        JsonNode review = read(root.resolve("validation").resolve("generated-code-review-result.json"));
        JsonNode generatedSmoke = read(root.resolve("validation").resolve("generated-ui-smoke-result.json"));
        JsonNode liveSmoke = read(root.resolve("validation").resolve("live-ui-smoke-result.json"));
        JsonNode flowFeedback = read(root.resolve("flow-contracts").resolve("flow-runtime-feedback.json"));

        String compileStatus = status(compile);
        String generatedSmokeStatus = status(generatedSmoke);
        String liveSmokeStatus = status(liveSmoke);
        String feedback = flowFeedback.isMissingNode()
                ? "missing-artifact"
                : booleanValue(flowFeedback, "success") ? "PASSED" : "FAILED";
        String dbRetrieval = dbRetrieval(summary);
        String outcome = outcome(compileStatus, review.path("totalFindings").asInt(0), generatedSmokeStatus,
                liveSmokeStatus, feedback);

        return new RunHistoryStatisticsRow(
                text(summary, "runId", root.getFileName().toString()),
                integer(summary, "requirements"),
                integer(summary, "canonicalTestCases"),
                integer(summary, "qualityScore"),
                dbRetrieval,
                integer(reuseTypes.path("POM_CONTRACT"), "hits"),
                integer(reuseTypes.path("POM_CONTRACT"), "misses"),
                integer(reuseTypes.path("FLOW_CONTRACT"), "hits"),
                integer(reuseTypes.path("FLOW_CONTRACT"), "misses"),
                integer(reuse, "llmCallsExecuted"),
                integer(reuse, "llmCallsSkipped"),
                integer(lifecycle, "stableArtifacts"),
                integer(lifecycle, "needsReviewArtifacts"),
                compileStatus,
                integer(review, "totalFindings"),
                generatedSmokeStatus,
                liveSmokeStatus,
                feedback,
                outcome
        );
    }

    private String markdown(List<RunHistoryStatisticsRow> rows, int limit) {
        StringBuilder table = new StringBuilder("# Last ").append(limit).append(" AI Runs\n\n")
                .append("Generated: ").append(Instant.now()).append("\n\n")
                .append("| Run | Req/TC | Quality | DB retrieval | POM reuse | Flow reuse | LLM exec/skip | Lifecycle stable/review | Compile | Review | Generated smoke | Live smoke | Flow DB feedback | Outcome |\n")
                .append("| --- | ---: | ---: | --- | --- | --- | --- | --- | --- | ---: | --- | --- | --- | --- |\n");
        for (RunHistoryStatisticsRow row : rows) {
            table.append("| ").append(escape(row.runId()))
                    .append(" | ").append(row.requirements()).append('/').append(row.canonicalTestCases())
                    .append(" | ").append(row.qualityScore())
                    .append(" | ").append(escape(row.dbRetrieval()))
                    .append(" | ").append(row.pomReuseHits()).append('/').append(row.pomReuseMisses())
                    .append(" | ").append(row.flowReuseHits()).append('/').append(row.flowReuseMisses())
                    .append(" | ").append(row.llmCallsExecuted()).append('/').append(row.llmCallsSkipped())
                    .append(" | ").append(row.lifecycleStable()).append('/').append(row.lifecycleNeedsReview())
                    .append(" | ").append(escape(row.compileStatus()))
                    .append(" | ").append(row.reviewFindings())
                    .append(" | ").append(escape(row.generatedSmokeStatus()))
                    .append(" | ").append(escape(row.liveSmokeStatus()))
                    .append(" | ").append(escape(row.flowFeedback()))
                    .append(" | ").append(escape(row.outcome())).append(" |\n");
        }
        if (rows.isEmpty()) {
            table.append("| No completed runs found | 0/0 | 0 | unavailable | 0/0 | 0/0 | 0/0 | 0/0 | missing-artifact | 0 | missing-artifact | missing-artifact | missing-artifact | WARNING |\n");
        }
        return table.toString();
    }

    private String outcome(String compile, int reviewFindings, String generatedSmoke, String liveSmoke, String feedback) {
        if ("FAILED".equals(compile) || "FAILED".equals(generatedSmoke) || "FAILED".equals(liveSmoke)
                || "FAILED".equals(feedback)) {
            return "FAILED";
        }
        if ("PASSED".equals(compile) && reviewFindings == 0 && "PASSED".equals(generatedSmoke)
                && "PASSED".equals(liveSmoke) && "PASSED".equals(feedback)) {
            return "PASSED";
        }
        return "WARNING";
    }

    private String dbRetrieval(JsonNode summary) {
        boolean neo4j = booleanValue(summary, "neo4jHit");
        boolean qdrant = booleanValue(summary, "qdrantHit");
        boolean stableCache = booleanValue(summary, "stableCacheUsed");
        String mode = text(summary, "retrievalMode", "unknown");
        return mode + " (neo4j=" + neo4j + ", qdrant=" + qdrant + ", stableCache=" + stableCache + ')';
    }

    private Path aiRunRoot(Path root) {
        return Files.isRegularFile(root.resolve("run-summary.json")) ? root : root.resolve("ai-run");
    }

    private boolean hasRunSummary(Path root) {
        return root != null && Files.isRegularFile(root.resolve("run-summary.json"));
    }

    private String runId(Path root) {
        return text(read(root.resolve("run-summary.json")), "runId", root.getFileName().toString());
    }

    private long lastModified(Path root) {
        try {
            return Files.getLastModifiedTime(root.resolve("run-summary.json")).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private JsonNode read(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return objectMapper.missingNode();
        }
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            return objectMapper.missingNode();
        }
    }

    private int integer(JsonNode node, String field) {
        return node == null || node.isMissingNode() ? 0 : Math.max(0, node.path(field).asInt(0));
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node == null || node.isMissingNode() ? "" : node.path(field).asText("");
        return value.isBlank() ? fallback : value.trim();
    }

    private String status(JsonNode node) {
        return node == null || node.isMissingNode() ? "missing-artifact" : text(node, "status", "unknown").toUpperCase();
    }

    private boolean booleanValue(JsonNode node, String field) {
        return node != null && !node.isMissingNode() && node.path(field).asBoolean(false);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|");
    }
}
