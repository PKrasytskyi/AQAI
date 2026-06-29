package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record InitialTestIdea(
        String id,
        InitialTestIdeaType type,
        String title,
        String rationale,
        List<String> relatedArtifacts
) {
    public InitialTestIdea {
        relatedArtifacts = relatedArtifacts == null ? List.of() : List.copyOf(relatedArtifacts);
    }
}
