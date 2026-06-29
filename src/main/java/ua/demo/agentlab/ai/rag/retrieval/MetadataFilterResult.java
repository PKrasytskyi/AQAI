package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.List;

public record MetadataFilterResult(
        List<RetrievedChunk> filteredChunks,
        int rejectedCount
) {
    public MetadataFilterResult {
        filteredChunks = filteredChunks == null ? List.of() : List.copyOf(filteredChunks);
        rejectedCount = Math.max(0, rejectedCount);
    }
}
