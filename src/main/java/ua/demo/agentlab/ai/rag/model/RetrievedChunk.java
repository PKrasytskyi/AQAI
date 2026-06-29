package ua.demo.agentlab.ai.rag.model;

public record RetrievedChunk(
        String chunkId,
        String relativePath,
        String language,
        int chunkIndex,
        double score,
        String text,
        ChunkMetadata metadata
) {
}
