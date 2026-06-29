package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.ArtifactType;

import java.util.List;
import java.util.Set;

public record TaskClassification(
        RagTaskType taskType,
        double confidenceScore,
        Set<String> signals,
        List<ArtifactType> preferredArtifactTypes
) {
    public TaskClassification {
        if (taskType == null) {
            throw new IllegalArgumentException("taskType cannot be null");
        }
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
        signals = signals == null ? Set.of() : Set.copyOf(signals);
        preferredArtifactTypes = preferredArtifactTypes == null ? List.of() : List.copyOf(preferredArtifactTypes);
    }
}
