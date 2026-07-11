package ua.demo.agentlab.ai.artifactdiff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.quality.AiRunQualitySummary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ArtifactDiffService {

    private static final Path TARGET = Path.of("target");
    private static final Path AI_RUN = TARGET.resolve("ai-run");
    private static final Path DISCOVERY = TARGET.resolve("discovery");
    private static final Path HISTORY = TARGET.resolve("ai-run-history");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArtifactDiffReport diffAndArchive(AiRunQualitySummary summary) {
        String currentRunId = summary == null ? "unknown-run" : summary.runId();
        Optional<Path> previousRun = latestHistoryRun();
        ArtifactDiffReport report = diff(currentRunId, previousRun);
        writeReport(report);
        return report;
    }

    private ArtifactDiffReport diff(String currentRunId, Optional<Path> previousRun) {
        List<ArtifactCategoryDiff> categories = new ArrayList<>();
        categories.add(compare("prompts", existingAiRunPath("page-objects", "page-object-spec"), previousRun,
                "ai-run/page-objects", "ai-run/page-object-spec"));
        categories.add(compare("test-prompts", AI_RUN.resolve("ui-test-spec"), previousRun, "ai-run/ui-test-spec"));
        categories.add(compare("mapped-pages", DISCOVERY.resolve("mapped-pages"), previousRun, "discovery/mapped-pages"));
        categories.add(compare("locator-candidates", DISCOVERY.resolve("mapped-ui-knowledge.json"), previousRun, "discovery/mapped-ui-knowledge.json"));
        categories.add(compare("enrichment-records", AI_RUN.resolve("enrichment/page-model-enrichments.json"), previousRun,
                "ai-run/enrichment/page-model-enrichments.json"));
        categories.add(compare("expected-results", AI_RUN.resolve("expectations/test-case-expected-results.json"), previousRun,
                "ai-run/expectations/test-case-expected-results.json"));
        categories.add(compare("quality-summary", AI_RUN.resolve("quality/run-quality-summary.json"), previousRun,
                "ai-run/quality/run-quality-summary.json"));

        List<ArtifactRegression> regressions = previousRun
                .map(path -> qualityRegressions(path.resolve("ai-run/quality/run-quality-summary.json"),
                        AI_RUN.resolve("quality/run-quality-summary.json")))
                .orElseGet(List::of);

        return new ArtifactDiffReport(
                currentRunId,
                previousRun.map(path -> path.getFileName().toString()).orElse(""),
                categories,
                regressions
        );
    }

    private ArtifactCategoryDiff compare(
            String category,
            Path currentPath,
            Optional<Path> previousRun,
            String previousRelativePath
    ) {
        return compare(category, currentPath, previousRun, previousRelativePath, previousRelativePath);
    }

    private ArtifactCategoryDiff compare(
            String category,
            Path currentPath,
            Optional<Path> previousRun,
            String previousRelativePath,
            String fallbackPreviousRelativePath
    ) {
        Path previousPath = previousRun
                .map(path -> existingPath(path.resolve(previousRelativePath), path.resolve(fallbackPreviousRelativePath)))
                .orElse(null);
        String currentHash = hash(currentPath);
        String previousHash = previousPath == null ? "" : hash(previousPath);
        String status;
        if (currentHash.isBlank() && previousHash.isBlank()) {
            status = "missing";
        } else if (previousHash.isBlank()) {
            status = "new";
        } else if (currentHash.isBlank()) {
            status = "removed";
        } else if (currentHash.equals(previousHash)) {
            status = "unchanged";
        } else {
            status = "changed";
        }
        return new ArtifactCategoryDiff(
                category,
                status,
                currentPath.toString(),
                previousPath == null ? "" : previousPath.toString(),
                currentHash,
                previousHash,
                "Hash is calculated over normalized file bytes for this artifact category"
        );
    }

    private Path existingAiRunPath(String primary, String fallback) {
        return existingPath(AI_RUN.resolve(primary), AI_RUN.resolve(fallback));
    }

    private Path existingPath(Path primary, Path fallback) {
        if (primary != null && Files.exists(primary)) {
            return primary;
        }
        return fallback;
    }

    private List<ArtifactRegression> qualityRegressions(Path previousSummary, Path currentSummary) {
        if (!Files.exists(previousSummary) || !Files.exists(currentSummary)) {
            return List.of();
        }
        try {
            JsonNode previous = objectMapper.readTree(previousSummary.toFile());
            JsonNode current = objectMapper.readTree(currentSummary.toFile());
            List<ArtifactRegression> regressions = new ArrayList<>();
            addIfLower(regressions, "quality-summary", "QUALITY_SCORE_DOWN", "qualityScore", previous, current);
            addIfHigher(regressions, "prompt-quality", "PROMPT_BLOCKERS_UP", "promptBlockingIssues", previous, current);
            addIfHigher(regressions, "expected-results", "EXPECTED_RESULTS_NEEDS_REVIEW_UP", "expectedResultsNeedsReview", previous, current);
            addIfHigher(regressions, "mapped-pages", "ROUTE_COLLISIONS_UP", "routeCollisions", previous, current);
            addIfHigher(regressions, "locator-candidates", "LOW_CONFIDENCE_LOCATORS_UP", "lowConfidenceLocators", previous, current);
            addIfLower(regressions, "locator-candidates", "AVERAGE_LOCATOR_SCORE_DOWN", "averageLocatorScore", previous, current);
            return List.copyOf(regressions);
        } catch (IOException exception) {
            return List.of(new ArtifactRegression(
                    "quality-summary",
                    "QUALITY_DIFF_UNAVAILABLE",
                    exception.getMessage(),
                    previousSummary.toString(),
                    currentSummary.toString()
            ));
        }
    }

    private void addIfHigher(
            List<ArtifactRegression> regressions,
            String category,
            String ruleId,
            String field,
            JsonNode previous,
            JsonNode current
    ) {
        double previousValue = previous.path(field).asDouble(0.0d);
        double currentValue = current.path(field).asDouble(0.0d);
        if (currentValue > previousValue) {
            regressions.add(new ArtifactRegression(
                    category,
                    ruleId,
                    field + " increased",
                    String.valueOf(previousValue),
                    String.valueOf(currentValue)
            ));
        }
    }

    private void addIfLower(
            List<ArtifactRegression> regressions,
            String category,
            String ruleId,
            String field,
            JsonNode previous,
            JsonNode current
    ) {
        double previousValue = previous.path(field).asDouble(0.0d);
        double currentValue = current.path(field).asDouble(0.0d);
        if (currentValue < previousValue) {
            regressions.add(new ArtifactRegression(
                    category,
                    ruleId,
                    field + " decreased",
                    String.valueOf(previousValue),
                    String.valueOf(currentValue)
            ));
        }
    }

    private Optional<Path> latestHistoryRun() {
        if (!Files.isDirectory(HISTORY)) {
            return Optional.empty();
        }
        try (Stream<Path> paths = Files.list(HISTORY)) {
            return paths
                    .filter(Files::isDirectory)
                    .max(Comparator.comparing(path -> {
                        try {
                            return Files.getLastModifiedTime(path);
                        } catch (IOException exception) {
                            return java.nio.file.attribute.FileTime.fromMillis(0);
                        }
                    }));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private void writeReport(ArtifactDiffReport report) {
        try {
            Files.createDirectories(AI_RUN.resolve("quality"));
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(AI_RUN.resolve("quality/artifact-diff.json").toFile(), report);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write artifact diff report", exception);
        }
    }

    private String hash(Path path) {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                        digest.update(path.relativize(file).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
                        digest.update(Files.readAllBytes(file));
                    }
                }
            } else {
                digest.update(Files.readAllBytes(path));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            return "";
        }
    }

}
