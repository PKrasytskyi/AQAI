package ua.demo.agentlab.ai.rag.model;

import java.nio.file.Path;
import java.util.List;

public record IndexedArtifact(
        Path absolutePath,
        String relativePath,
        String language,
        ArtifactType artifactType,
        String artifactName,
        String packageName,
        List<String> tags
) {
    public IndexedArtifact {
        if (absolutePath == null) {
            throw new IllegalArgumentException("absolutePath cannot be null");
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath cannot be blank");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language cannot be blank");
        }
        artifactType = artifactType == null ? ArtifactType.UNKNOWN : artifactType;
        artifactName = artifactName == null ? "" : artifactName.trim();
        packageName = packageName == null ? "" : packageName.trim();
        tags = tags == null ? List.of() : List.copyOf(tags);
        absolutePath = absolutePath.toAbsolutePath().normalize();
    }

    public ChunkMetadata toChunkMetadata() {
        return new ChunkMetadata(artifactType, artifactName, packageName, tags);
    }
}
