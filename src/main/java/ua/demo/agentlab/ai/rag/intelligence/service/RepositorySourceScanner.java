package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public class RepositorySourceScanner {

    private static final Set<String> SUPPORTED_FILE_NAMES = Set.of("pom.xml");
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".java", ".md", ".feature", ".yaml", ".yml", ".json", ".properties"
    );
    private static final Set<String> IGNORED_SEGMENTS = Set.of(".git", "target", ".m2repo", ".idea");

    private final RepositoryIntelligenceConfig config;

    public RepositorySourceScanner(RepositoryIntelligenceConfig config) {
        this.config = config;
    }

    public List<SourceDocument> scan(Path workspaceRoot) {
        Path normalizedRoot = workspaceRoot.toAbsolutePath().normalize();
        List<SourceDocument> documents = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(normalizedRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .filter(path -> !isIgnored(path))
                    .forEach(path -> documents.add(readDocument(normalizedRoot, path)));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan repository: " + normalizedRoot, exception);
        }
        return documents;
    }

    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString();
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        return SUPPORTED_FILE_NAMES.contains(fileName) || SUPPORTED_EXTENSIONS.stream().anyMatch(lowerFileName::endsWith);
    }

    private boolean isIgnored(Path path) {
        for (Path segment : path) {
            if (IGNORED_SEGMENTS.contains(segment.toString())) {
                return true;
            }
        }
        if (!config.includeGeneratedArtifacts()) {
            String normalized = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
            if (normalized.contains("/src/test/java/ua/demo/agentlab/ui/generated/")
                    || normalized.contains("/src/main/java/ua/demo/agentlab/ui/generated/")) {
                return true;
            }
        }
        return false;
    }

    private SourceDocument readDocument(Path root, Path path) {
        try {
            return new SourceDocument(
                    path,
                    root.relativize(path).toString().replace('\\', '/'),
                    detectLanguage(path),
                    Files.readString(path, StandardCharsets.UTF_8)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read repository file: " + path, exception);
        }
    }

    private String detectLanguage(Path path) {
        String lowerFileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lowerFileName.endsWith(".java")) {
            return "java";
        }
        if (lowerFileName.endsWith(".feature")) {
            return "gherkin";
        }
        if (lowerFileName.endsWith(".yaml") || lowerFileName.endsWith(".yml")) {
            return "yaml";
        }
        if (lowerFileName.endsWith(".json")) {
            return "json";
        }
        if (lowerFileName.endsWith(".properties")) {
            return "properties";
        }
        if (lowerFileName.endsWith(".xml")) {
            return "xml";
        }
        return "markdown";
    }
}
