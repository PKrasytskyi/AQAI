package ua.demo.agentlab.ui.discovery.knowledge.model;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public record MappedUiKnowledgeCurated(
        MappedUiKnowledge knowledge,
        List<ExcludedEvidence> excludedEvidence,
        List<String> sourceTrace,
        double confidence
) {
    public MappedUiKnowledgeCurated {
        knowledge = knowledge == null ? MappedUiKnowledge.empty() : knowledge;
        excludedEvidence = excludedEvidence == null ? List.of() : List.copyOf(excludedEvidence);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }
}
