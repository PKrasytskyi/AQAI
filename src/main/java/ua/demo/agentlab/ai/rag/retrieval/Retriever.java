package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.store.VectorStore;

import java.util.List;

public class Retriever {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final int retrievalLimit;

    public Retriever(EmbeddingService embeddingService, VectorStore vectorStore, int retrievalLimit) {
        if (embeddingService == null || vectorStore == null) {
            throw new IllegalArgumentException("Retriever dependencies cannot be null");
        }
        if (retrievalLimit <= 0) {
            throw new IllegalArgumentException("retrievalLimit must be positive");
        }
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.retrievalLimit = retrievalLimit;
    }

    public List<RetrievedChunk> retrieve(String request) {
        return retrieve(request, retrievalLimit);
    }

    public List<RetrievedChunk> retrieve(String request, int limit) {
        if (request == null || request.isBlank()) {
            throw new IllegalArgumentException("request cannot be blank");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return vectorStore.search(embeddingService.embed(request), limit);
    }
}
