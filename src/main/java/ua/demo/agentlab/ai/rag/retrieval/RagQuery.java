package ua.demo.agentlab.ai.rag.retrieval;

import java.util.List;

public record RagQuery(
        String semanticQuery,
        List<String> graphTerms,
        List<String> metadataHints
) {
    public RagQuery {
        if (semanticQuery == null || semanticQuery.isBlank()) {
            throw new IllegalArgumentException("semanticQuery cannot be blank");
        }
        graphTerms = graphTerms == null ? List.of() : List.copyOf(graphTerms);
        metadataHints = metadataHints == null ? List.of() : List.copyOf(metadataHints);
    }
}
