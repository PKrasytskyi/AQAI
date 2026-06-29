package ua.demo.agentlab.ai.context;

import java.util.Map;

public record UiKnowledgeGraphMatch(
        String nodeId,
        String nodeType,
        String name,
        String pageId,
        String relationType,
        double score,
        Map<String, String> metadata
) {
    public UiKnowledgeGraphMatch {
        nodeId = nodeId == null ? "" : nodeId.trim();
        nodeType = nodeType == null ? "" : nodeType.trim();
        name = name == null ? "" : name.trim();
        pageId = pageId == null ? "" : pageId.trim();
        relationType = relationType == null ? "" : relationType.trim();
        score = Math.max(0.0d, score);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
