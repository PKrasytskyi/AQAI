package ua.demo.agentlab.ui.discovery.mapping.model;

import java.util.List;
import java.util.Map;

public record PageKnowledgeVectorDocument(
        String documentId,
        String documentType,
        String sourcePageId,
        String sourceEntityId,
        String text,
        List<String> keywords,
        Map<String, String> metadata
) {
    public PageKnowledgeVectorDocument(
            String documentId,
            String documentType,
            String sourcePageId,
            String sourceEntityId,
            String text,
            List<String> keywords
    ) {
        this(documentId, documentType, sourcePageId, sourceEntityId, text, keywords, Map.of());
    }

    public PageKnowledgeVectorDocument {
        documentId = documentId == null ? "" : documentId.trim();
        documentType = documentType == null ? "" : documentType.trim();
        sourcePageId = sourcePageId == null ? "" : sourcePageId.trim();
        sourceEntityId = sourceEntityId == null ? "" : sourceEntityId.trim();
        text = text == null ? "" : text.trim();
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
