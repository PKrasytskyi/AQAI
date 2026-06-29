package ua.demo.agentlab.ui.discovery.mapping.model;

import java.util.Map;

public record PageKnowledgeGraphNode(
        String nodeId,
        String nodeType,
        String name,
        String pageId,
        Map<String, String> metadata
) {
    public PageKnowledgeGraphNode {
        nodeId = nodeId == null ? "" : nodeId.trim();
        nodeType = nodeType == null ? "" : nodeType.trim();
        name = name == null ? "" : name.trim();
        pageId = pageId == null ? "" : pageId.trim();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
