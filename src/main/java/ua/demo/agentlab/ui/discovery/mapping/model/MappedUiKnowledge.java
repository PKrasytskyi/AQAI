package ua.demo.agentlab.ui.discovery.mapping.model;

import java.util.List;

public record MappedUiKnowledge(
        List<MappedPage> pages,
        List<MappedTransition> transitions,
        List<PageKnowledgeGraphNode> graphNodes,
        List<PageKnowledgeGraphEdge> graphEdges,
        List<PageKnowledgeVectorDocument> vectorDocuments
) {
    public static MappedUiKnowledge empty() {
        return new MappedUiKnowledge(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public MappedUiKnowledge {
        pages = pages == null ? List.of() : List.copyOf(pages);
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
        graphNodes = graphNodes == null ? List.of() : List.copyOf(graphNodes);
        graphEdges = graphEdges == null ? List.of() : List.copyOf(graphEdges);
        vectorDocuments = vectorDocuments == null ? List.of() : List.copyOf(vectorDocuments);
    }
}
