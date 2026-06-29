package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.List;

public record ContextRetrievalResult(
        QueryIntent intent,
        RagQuery ragQuery,
        RetrievalPolicy retrievalPolicy,
        RetrievalTrace retrievalTrace,
        java.util.List<ContextRerankExplanation> rerankExplanations,
        List<RetrievedChunk> contextChunks,
        int rawMatchCount,
        int graphExpandedCount,
        int filteredCandidateCount,
        String graphSource
) {
    public ContextRetrievalResult {
        if (intent == null) {
            throw new IllegalArgumentException("intent cannot be null");
        }
        if (ragQuery == null) {
            throw new IllegalArgumentException("ragQuery cannot be null");
        }
        if (retrievalPolicy == null) {
            throw new IllegalArgumentException("retrievalPolicy cannot be null");
        }
        if (retrievalTrace == null) {
            throw new IllegalArgumentException("retrievalTrace cannot be null");
        }
        rerankExplanations = rerankExplanations == null ? java.util.List.of() : java.util.List.copyOf(rerankExplanations);
        contextChunks = contextChunks == null ? List.of() : List.copyOf(contextChunks);
        if (rawMatchCount < 0) {
            throw new IllegalArgumentException("rawMatchCount cannot be negative");
        }
        if (graphExpandedCount < 0) {
            throw new IllegalArgumentException("graphExpandedCount cannot be negative");
        }
        if (filteredCandidateCount < 0) {
            throw new IllegalArgumentException("filteredCandidateCount cannot be negative");
        }
        graphSource = graphSource == null ? "" : graphSource.trim();
    }
}
