package ua.demo.agentlab.ui.discovery.semanticgraph.model;

public record SemanticGraphEdge(
        String fromId,
        String toId,
        String edgeType,
        double confidence
) {
    public SemanticGraphEdge {
        fromId = safe(fromId);
        toId = safe(toId);
        edgeType = safe(edgeType);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
