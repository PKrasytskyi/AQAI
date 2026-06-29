package ua.demo.agentlab.ai.rag.retrieval;

public record RetrievalTrace(
        String userRequest,
        RagTaskType taskType,
        double taskConfidence,
        String semanticQuery,
        int requestedArtifacts,
        int rawSemanticMatches,
        int semanticSeedCount,
        int graphExpandedArtifacts,
        int filteredCandidates,
        int finalArtifacts,
        String graphSource
) {
    public RetrievalTrace {
        userRequest = userRequest == null ? "" : userRequest.trim();
        if (taskType == null) {
            throw new IllegalArgumentException("taskType cannot be null");
        }
        taskConfidence = Math.max(0.0d, Math.min(1.0d, taskConfidence));
        semanticQuery = semanticQuery == null ? "" : semanticQuery.trim();
        requestedArtifacts = Math.max(0, requestedArtifacts);
        rawSemanticMatches = Math.max(0, rawSemanticMatches);
        semanticSeedCount = Math.max(0, semanticSeedCount);
        graphExpandedArtifacts = Math.max(0, graphExpandedArtifacts);
        filteredCandidates = Math.max(0, filteredCandidates);
        finalArtifacts = Math.max(0, finalArtifacts);
        graphSource = graphSource == null ? "" : graphSource.trim();
    }
}
