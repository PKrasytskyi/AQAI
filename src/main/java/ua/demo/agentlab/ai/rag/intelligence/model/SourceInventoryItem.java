package ua.demo.agentlab.ai.rag.intelligence.model;

public record SourceInventoryItem(
        String relativePath,
        String language,
        long charCount
) {
}
