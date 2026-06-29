package ua.demo.agentlab.ai.rag.retrieval;

public record ContextRetrievalRequest(
        String userRequest,
        int maxArtifacts
) {
}
