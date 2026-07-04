package ua.demo.agentlab.ui.discovery.semanticgraph.model;

import java.util.List;

public record SemanticGraphModel(
        List<SemanticGraphNode> nodes,
        List<SemanticGraphEdge> edges,
        List<String> sourceTrace
) {
    public SemanticGraphModel {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static SemanticGraphModel empty(String reason) {
        return new SemanticGraphModel(List.of(), List.of(), List.of(reason == null ? "semantic-graph:empty" : reason));
    }
}
