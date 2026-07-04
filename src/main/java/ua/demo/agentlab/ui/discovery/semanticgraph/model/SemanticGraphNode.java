package ua.demo.agentlab.ui.discovery.semanticgraph.model;

import java.util.Map;

public record SemanticGraphNode(
        String nodeId,
        String nodeType,
        String name,
        String pageId,
        Map<String, String> metadata
) {
    public SemanticGraphNode {
        nodeId = safe(nodeId);
        nodeType = safe(nodeType);
        name = safe(name);
        pageId = safe(pageId);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
