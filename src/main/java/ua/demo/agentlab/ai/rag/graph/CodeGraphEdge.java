package ua.demo.agentlab.ai.rag.graph;

public record CodeGraphEdge(
        String fromNodeId,
        String toNodeId,
        GraphEdgeType edgeType
) {
    public CodeGraphEdge {
        if (fromNodeId == null || fromNodeId.isBlank()) {
            throw new IllegalArgumentException("fromNodeId cannot be blank");
        }
        if (toNodeId == null || toNodeId.isBlank()) {
            throw new IllegalArgumentException("toNodeId cannot be blank");
        }
        if (edgeType == null) {
            throw new IllegalArgumentException("edgeType cannot be null");
        }
    }
}
