package ua.demo.agentlab.ai.rag.retrieval;

public record RetrievalPolicy(
        RagTaskType taskType,
        int desiredArtifacts,
        int semanticRetrievalMultiplier,
        int graphExpansionMultiplier,
        boolean keepSupportArtifacts,
        boolean preferDocumentation,
        boolean allowSyntheticGraphArtifacts
) {
    public RetrievalPolicy {
        if (taskType == null) {
            throw new IllegalArgumentException("taskType cannot be null");
        }
        desiredArtifacts = desiredArtifacts <= 0 ? 6 : desiredArtifacts;
        semanticRetrievalMultiplier = semanticRetrievalMultiplier <= 0 ? 3 : semanticRetrievalMultiplier;
        graphExpansionMultiplier = graphExpansionMultiplier <= 0 ? 2 : graphExpansionMultiplier;
    }
}
