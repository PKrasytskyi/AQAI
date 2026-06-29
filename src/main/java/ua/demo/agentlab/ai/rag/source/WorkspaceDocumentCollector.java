package ua.demo.agentlab.ai.rag.source;

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

public class WorkspaceDocumentCollector {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".java", ".md", ".feature");
    private static final Set<String> IGNORED_SEGMENTS = Set.of(".git", "target", ".m2repo", ".idea");

    public List<SourceDocument> collect(Path workspaceRoot) {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("workspaceRoot cannot be null");
        }

        Path normalizedRoot = workspaceRoot.toAbsolutePath().normalize();
        if (!Files.exists(normalizedRoot)) {
            throw new IllegalStateException("Workspace root does not exist: " + normalizedRoot);
        }

        List<SourceDocument> documents = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(normalizedRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .filter(path -> !isIgnored(path))
                    .forEach(path -> documents.add(readDocument(normalizedRoot, path)));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to collect workspace documents from: " + normalizedRoot, exception);
        }

        return documents;
    }

    private SourceDocument readDocument(Path workspaceRoot, Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return new SourceDocument(
                    path,
                    workspaceRoot.relativize(path).toString().replace('\\', '/'),
                    detectLanguage(path),
                    content
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read document: " + path, exception);
        }
    }

    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private boolean isIgnored(Path path) {
        for (Path segment : path) {
            if (IGNORED_SEGMENTS.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private String detectLanguage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".java")) {
            return "java";
        }
        if (fileName.endsWith(".feature")) {
            return "gherkin";
        }
        return "markdown";
    }
}
