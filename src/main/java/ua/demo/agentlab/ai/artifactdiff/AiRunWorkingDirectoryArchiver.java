package ua.demo.agentlab.ai.artifactdiff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class AiRunWorkingDirectoryArchiver {

    private static final Path TARGET = Path.of("target");
    private static final Path AI_RUN = TARGET.resolve("ai-run");
    private static final Path DISCOVERY = TARGET.resolve("discovery");
    private static final Path HISTORY = TARGET.resolve("ai-run-history");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void archiveAndCleanBeforeRun() {
        if (!Files.exists(AI_RUN) && !Files.exists(DISCOVERY)) {
            return;
        }
        String runId = previousRunId();
        Path runDirectory = uniqueRunDirectory(runId);
        try {
            Files.createDirectories(runDirectory);
            copyIfExists(AI_RUN, runDirectory.resolve("ai-run"));
            copyIfExists(DISCOVERY, runDirectory.resolve("discovery"));
            deleteIfExists(AI_RUN);
            deleteIfExists(DISCOVERY);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to archive previous AI run artifacts", exception);
        }
    }

    private String previousRunId() {
        Path summary = AI_RUN.resolve("quality").resolve("run-quality-summary.json");
        if (Files.exists(summary)) {
            try {
                JsonNode root = objectMapper.readTree(summary.toFile());
                String runId = root.path("runId").asText("");
                if (!runId.isBlank()) {
                    return sanitize(runId);
                }
            } catch (IOException ignored) {
                // Fall back to timestamp.
            }
        }
        return "pre-run-" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
    }

    private Path uniqueRunDirectory(String runId) {
        Path candidate = HISTORY.resolve(sanitize(runId));
        if (!Files.exists(candidate)) {
            return candidate;
        }
        int index = 2;
        while (Files.exists(HISTORY.resolve(sanitize(runId) + "-" + index))) {
            index++;
        }
        return HISTORY.resolve(sanitize(runId) + "-" + index);
    }

    private void copyIfExists(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path candidate : ordered) {
                Files.deleteIfExists(candidate);
            }
        }
    }

    private String sanitize(String value) {
        String normalized = value == null || value.isBlank() ? "unknown-run" : value.trim();
        return normalized.replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
