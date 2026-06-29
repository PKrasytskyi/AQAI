package ua.demo.agentlab.ai.rag.graph;

import java.util.List;

public record CodeGraphNode(
        String id,
        GraphNodeType nodeType,
        String relativePath,
        String displayName,
        String packageName,
        List<String> tags,
        String previewText
) {
    public CodeGraphNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        nodeType = nodeType == null ? GraphNodeType.UNKNOWN : nodeType;
        relativePath = relativePath == null ? "" : relativePath;
        displayName = displayName == null ? "" : displayName;
        packageName = packageName == null ? "" : packageName;
        tags = tags == null ? List.of() : List.copyOf(tags);
        previewText = previewText == null ? "" : previewText;
    }
}
