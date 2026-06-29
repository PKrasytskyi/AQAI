package ua.demo.agentlab.ai.rag.model;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record RagChunk(
        String id,
        Path absolutePath,
        String relativePath,
        String language,
        int chunkIndex,
        int startOffset,
        int endOffset,
        String text
) {
    public RagChunk {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (absolutePath == null) {
            throw new IllegalArgumentException("absolutePath cannot be null");
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath cannot be blank");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language cannot be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text cannot be blank");
        }
        absolutePath = absolutePath.toAbsolutePath().normalize();
    }

    public Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("relativePath", relativePath);
        payload.put("absolutePath", absolutePath.toString());
        payload.put("language", language);
        payload.put("chunkIndex", chunkIndex);
        payload.put("startOffset", startOffset);
        payload.put("endOffset", endOffset);
        payload.put("text", text);
        return payload;
    }
}
