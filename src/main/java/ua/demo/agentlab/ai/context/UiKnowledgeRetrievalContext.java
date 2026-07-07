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
        List<String> notes,
        boolean neo4jHit,
        boolean qdrantHit,
        String retrievalMode,
        boolean stableCacheUsed,
        int staleEvidenceRejected,
        String vectorUnavailableReason
) {
    public UiKnowledgeRetrievalContext(
            String query,
            List<String> queryTerms,
            List<RetrievedChunk> vectorMatches,
            List<UiKnowledgeGraphMatch> graphMatches,
            String vectorSource,
            String graphSource,
            List<String> notes
    ) {
        this(
                query,
                queryTerms,
                vectorMatches,
                graphMatches,
                vectorSource,
                graphSource,
                notes,
                graphMatches != null && !graphMatches.isEmpty(),
                vectorMatches != null && !vectorMatches.isEmpty(),
                inferRetrievalMode(notes),
                hasNote(notes, "stable-page-cache") || hasNote(notes, "STABLE_PAGE_CACHE"),
                0,
                vectorUnavailableReason(vectorSource, notes)
        );
    }

    public UiKnowledgeRetrievalContext {
        query = query == null ? "" : query.trim();
        queryTerms = queryTerms == null ? List.of() : List.copyOf(queryTerms);
        vectorMatches = vectorMatches == null ? List.of() : List.copyOf(vectorMatches);
        graphMatches = graphMatches == null ? List.of() : List.copyOf(graphMatches);
        vectorSource = vectorSource == null ? "" : vectorSource.trim();
        graphSource = graphSource == null ? "" : graphSource.trim();
        notes = notes == null ? List.of() : List.copyOf(notes);
        retrievalMode = retrievalMode == null || retrievalMode.isBlank()
                ? "current-run"
                : retrievalMode.trim();
        staleEvidenceRejected = Math.max(0, staleEvidenceRejected);
        vectorUnavailableReason = vectorUnavailableReason == null ? "" : vectorUnavailableReason.trim();
    }

    public static UiKnowledgeRetrievalContext empty(String note) {
        return new UiKnowledgeRetrievalContext(
                "",
                List.of(),
                List.of(),
                List.of(),
                "DISABLED",
                "DISABLED",
                note == null || note.isBlank() ? List.of() : List.of(note),
                false,
                false,
                "disabled",
                false,
                0,
                ""
        );
    }

    private static String inferRetrievalMode(List<String> notes) {
        String joined = notes == null ? "" : String.join(" ", notes).toLowerCase(java.util.Locale.ROOT);
        if (joined.contains("stable_page_cache") || joined.contains("stable-page-cache")) {
            return "stable-page-cache";
        }
        if (joined.contains("repository") || joined.contains("code rag")) {
            return "repository-code";
        }
        if (joined.contains("current_run_only") || joined.contains("current-run")) {
            return "current-run";
        }
        return "current-run";
    }

    private static boolean hasNote(List<String> notes, String fragment) {
        if (notes == null || fragment == null || fragment.isBlank()) {
            return false;
        }
        String normalized = fragment.toLowerCase(java.util.Locale.ROOT);
        return notes.stream()
                .map(note -> note == null ? "" : note.toLowerCase(java.util.Locale.ROOT))
                .anyMatch(note -> note.contains(normalized));
    }

    private static String vectorUnavailableReason(String vectorSource, List<String> notes) {
        String source = vectorSource == null ? "" : vectorSource.trim();
        if (!source.toUpperCase(java.util.Locale.ROOT).contains("UNAVAILABLE")) {
            return "";
        }
        if (notes == null) {
            return source;
        }
        return notes.stream()
                .filter(note -> note != null && note.toLowerCase(java.util.Locale.ROOT).contains("vector retrieval unavailable"))
                .findFirst()
                .orElse(source);
    }
}
