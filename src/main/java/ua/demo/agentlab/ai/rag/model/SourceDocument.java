package ua.demo.agentlab.ai.rag.model;

import java.nio.file.Path;

public record SourceDocument(
        Path path,
        String relativePath,
        String language,
        String content
) {
    public SourceDocument {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath cannot be blank");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language cannot be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        path = path.toAbsolutePath().normalize();
    }
}
