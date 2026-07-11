package ua.demo.agentlab.ai.debug;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class AiRunArtifactWriter {

    private static final Path ROOT = Path.of("target", "ai-run");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Path writeText(String stage, String fileName, String content) {
        try {
            Path directory = ensureStageDirectory(stage);
            Path target = directory.resolve(sanitizeFileName(fileName));
            Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
            return target.toAbsolutePath().normalize();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write AI text artifact for stage '%s'".formatted(stage), exception);
        }
    }

    public Optional<Path> writeDebugText(String stage, String fileName, String content) {
        if (!debugArtifactsEnabled()) {
            return Optional.empty();
        }
        return Optional.of(writeText(debugStage(stage), fileName, content));
    }

    public Path writeJson(String stage, String fileName, Object payload) {
        try {
            Path directory = ensureStageDirectory(stage);
            Path target = directory.resolve(sanitizeFileName(fileName));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), payload);
            return target.toAbsolutePath().normalize();
        } catch (IOException exception) {
            String detail = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            throw new IllegalStateException(
                    "Failed to write AI JSON artifact for stage '%s': %s".formatted(stage, detail),
                    exception
            );
        }
    }

    public Optional<Path> writeDebugJson(String stage, String fileName, Object payload) {
        if (!debugArtifactsEnabled()) {
            return Optional.empty();
        }
        return Optional.of(writeJson(debugStage(stage), fileName, payload));
    }

    public boolean debugArtifactsEnabled() {
        String property = System.getProperty("ai.debug.artifacts");
        if (property != null && !property.isBlank()) {
            return Boolean.parseBoolean(property.trim());
        }
        String env = System.getenv("AI_DEBUG_ARTIFACTS");
        return env != null && Boolean.parseBoolean(env.trim());
    }

    private Path ensureStageDirectory(String stage) throws IOException {
        Path directory = stage == null || stage.isBlank()
                ? ROOT
                : ROOT.resolve(sanitizeSegment(stage));
        Files.createDirectories(directory);
        return directory;
    }

    private String debugStage(String stage) {
        return "debug/" + sanitizeSegment(stage);
    }

    private String sanitizeFileName(String value) {
        String sanitized = sanitizeSegment(value);
        if (sanitized.isBlank()) {
            return "artifact.txt";
        }
        return sanitized;
    }

    private String sanitizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "default";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
