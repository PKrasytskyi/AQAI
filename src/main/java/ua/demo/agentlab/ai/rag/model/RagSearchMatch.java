package ua.demo.agentlab.ai.rag.model;

public record RagSearchMatch(
        String chunkId,
        String relativePath,
        String language,
        int chunkIndex,
        double score,
        String text
) {
}
