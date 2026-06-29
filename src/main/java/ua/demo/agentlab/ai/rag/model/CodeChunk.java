package ua.demo.agentlab.ai.rag.model;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record CodeChunk(
        String id,
        Path absolutePath,
        String relativePath,
        String language,
        int chunkIndex,
        int startOffset,
        int endOffset,
        String text,
        ChunkMetadata metadata
) {
    public CodeChunk {
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
        metadata = metadata == null ? new ChunkMetadata(ArtifactType.UNKNOWN, "", "", java.util.List.of()) : metadata;
        absolutePath = absolutePath.toAbsolutePath().normalize();
    }

    public static CodeChunk fromLegacy(RagChunk ragChunk, ChunkMetadata metadata) {
        if (ragChunk == null) {
            throw new IllegalArgumentException("ragChunk cannot be null");
        }
        return new CodeChunk(
                ragChunk.id(),
                ragChunk.absolutePath(),
                ragChunk.relativePath(),
                ragChunk.language(),
                ragChunk.chunkIndex(),
                ragChunk.startOffset(),
                ragChunk.endOffset(),
                ragChunk.text(),
                metadata
        );
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
        payload.put("artifactType", metadata.artifactType().name());
        payload.put("artifactName", metadata.artifactName());
        payload.put("packageName", metadata.packageName());
        payload.put("tags", metadata.tags());
        payload.putAll(metadata.attributes());
        return payload;
    }
}
