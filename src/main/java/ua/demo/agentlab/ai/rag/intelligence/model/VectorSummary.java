package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record VectorSummary(
        String id,
        String sourcePath,
        String summaryType,
        String summaryText,
        List<String> keywords
) {
    public VectorSummary {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
