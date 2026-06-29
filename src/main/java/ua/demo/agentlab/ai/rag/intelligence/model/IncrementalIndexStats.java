package ua.demo.agentlab.ai.rag.intelligence.model;

public record IncrementalIndexStats(
        boolean reusedPreviousSnapshot,
        int changedDocuments,
        int unchangedDocuments,
        int newDocuments,
        int removedDocuments
) {
    public IncrementalIndexStats {
        changedDocuments = Math.max(0, changedDocuments);
        unchangedDocuments = Math.max(0, unchangedDocuments);
        newDocuments = Math.max(0, newDocuments);
        removedDocuments = Math.max(0, removedDocuments);
    }
}
