package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.List;

public record UiKnowledgeRetrievalContext(
        String query,
        List<String> queryTerms,
        List<RetrievedChunk> vectorMatches,
        List<UiKnowledgeGraphMatch> graphMatches,
        String vectorSource,
        String graphSource,
        List<String> notes
) {
    public UiKnowledgeRetrievalContext {
        query = query == null ? "" : query.trim();
        queryTerms = queryTerms == null ? List.of() : List.copyOf(queryTerms);
        vectorMatches = vectorMatches == null ? List.of() : List.copyOf(vectorMatches);
        graphMatches = graphMatches == null ? List.of() : List.copyOf(graphMatches);
        vectorSource = vectorSource == null ? "" : vectorSource.trim();
        graphSource = graphSource == null ? "" : graphSource.trim();
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public static UiKnowledgeRetrievalContext empty(String note) {
        return new UiKnowledgeRetrievalContext(
                "",
                List.of(),
                List.of(),
                List.of(),
                "DISABLED",
                "DISABLED",
                note == null || note.isBlank() ? List.of() : List.of(note)
        );
    }
}
