package ua.demo.agentlab.ai.debug;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    private Path ensureStageDirectory(String stage) throws IOException {
        Path directory = ROOT.resolve(sanitizeSegment(stage));
        Files.createDirectories(directory);
        return directory;
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
