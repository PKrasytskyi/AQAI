package ua.demo.agentlab.ai.rag.intelligence.model;

public record ConfidenceAssessment(
        String dimension,
        double score,
        String rationale
) {
    public ConfidenceAssessment {
        score = Math.max(0.0d, Math.min(1.0d, score));
        dimension = dimension == null ? "" : dimension.trim();
        rationale = rationale == null ? "" : rationale.trim();
    }
}
