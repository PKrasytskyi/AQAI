package ua.demo.agentlab.ai.rag.intelligence.model;

public record RepositoryGraphRelation(
        String fromId,
        String toId,
        RepositoryGraphRelationType type
) {
}
