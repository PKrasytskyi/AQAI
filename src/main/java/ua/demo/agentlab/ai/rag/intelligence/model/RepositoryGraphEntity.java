package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record RepositoryGraphEntity(
        String id,
        RepositoryGraphEntityType type,
        String name,
        String relativePath,
        List<String> tags
) {
    public RepositoryGraphEntity {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
