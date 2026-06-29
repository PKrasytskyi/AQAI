package ua.demo.agentlab.ai.rag.intelligence.model;

public record ParserMetric(
        String parserName,
        int documentsSeen,
        int candidateDocuments,
        int extractedArtifacts,
        int reusedArtifacts,
        double confidenceScore
) {
    public ParserMetric {
        parserName = parserName == null ? "" : parserName.trim();
        documentsSeen = Math.max(0, documentsSeen);
        candidateDocuments = Math.max(0, candidateDocuments);
        extractedArtifacts = Math.max(0, extractedArtifacts);
        reusedArtifacts = Math.max(0, reusedArtifacts);
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }
}
