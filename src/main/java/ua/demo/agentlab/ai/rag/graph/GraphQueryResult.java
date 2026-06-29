package ua.demo.agentlab.ai.rag.graph;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.List;

public record GraphQueryResult(
        List<RetrievedChunk> relatedChunks,
        int matchedSeedCount,
        int expandedArtifactCount,
        String source
) {
    public GraphQueryResult {
        relatedChunks = relatedChunks == null ? List.of() : List.copyOf(relatedChunks);
        matchedSeedCount = Math.max(0, matchedSeedCount);
        expandedArtifactCount = Math.max(0, expandedArtifactCount);
        source = source == null ? "" : source.trim();
    }
}
