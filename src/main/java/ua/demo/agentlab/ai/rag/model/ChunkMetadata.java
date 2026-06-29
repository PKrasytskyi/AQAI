package ua.demo.agentlab.ai.rag.model;

import java.util.List;
import java.util.Map;

public record ChunkMetadata(
        ArtifactType artifactType,
        String artifactName,
        String packageName,
        List<String> tags,
        Map<String, String> attributes
) {
    public ChunkMetadata(
            ArtifactType artifactType,
            String artifactName,
            String packageName,
            List<String> tags
    ) {
        this(artifactType, artifactName, packageName, tags, Map.of());
    }

    public ChunkMetadata {
        artifactType = artifactType == null ? ArtifactType.UNKNOWN : artifactType;
        artifactName = artifactName == null ? "" : artifactName.trim();
        packageName = packageName == null ? "" : packageName.trim();
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
