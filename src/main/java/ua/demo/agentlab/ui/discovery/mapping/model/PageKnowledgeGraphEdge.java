package ua.demo.agentlab.ui.discovery.mapping.model;

public record PageKnowledgeGraphEdge(
        String fromId,
        String toId,
        String edgeType
) {
    public PageKnowledgeGraphEdge {
        fromId = fromId == null ? "" : fromId.trim();
        toId = toId == null ? "" : toId.trim();
        edgeType = edgeType == null ? "" : edgeType.trim();
    }
}
