package ua.demo.agentlab.ai.rag.retrieval;

import java.util.List;

public record ContextRerankExplanation(
        String relativePath,
        double finalScore,
        List<String> reasons
) {
    public ContextRerankExplanation {
        relativePath = relativePath == null ? "" : relativePath.trim();
        finalScore = Math.max(0.0d, finalScore);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
