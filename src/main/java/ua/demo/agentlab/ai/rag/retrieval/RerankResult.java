package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.List;

public record RerankResult(
        List<RetrievedChunk> rerankedChunks,
        List<ContextRerankExplanation> explanations
) {
    public RerankResult {
        rerankedChunks = rerankedChunks == null ? List.of() : List.copyOf(rerankedChunks);
        explanations = explanations == null ? List.of() : List.copyOf(explanations);
    }
}
