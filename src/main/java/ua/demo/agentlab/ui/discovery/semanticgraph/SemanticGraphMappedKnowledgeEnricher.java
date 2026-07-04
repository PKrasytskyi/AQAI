package ua.demo.agentlab.ui.discovery.semanticgraph;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticActionModel;
import ua.demo.agentlab.ui.discovery.semanticgraph.model.SemanticGraphEdge;
import ua.demo.agentlab.ui.discovery.semanticgraph.model.SemanticGraphModel;
import ua.demo.agentlab.ui.discovery.semanticgraph.model.SemanticGraphNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SemanticGraphMappedKnowledgeEnricher {

    private final SemanticActionModelBuilder semanticActionModelBuilder;
    private final SemanticGraphBuilder semanticGraphBuilder;

    public SemanticGraphMappedKnowledgeEnricher() {
        this(new SemanticActionModelBuilder(), new SemanticGraphBuilder());
    }

    public SemanticGraphMappedKnowledgeEnricher(
            SemanticActionModelBuilder semanticActionModelBuilder,
            SemanticGraphBuilder semanticGraphBuilder
    ) {
        this.semanticActionModelBuilder = semanticActionModelBuilder == null
                ? new SemanticActionModelBuilder()
                : semanticActionModelBuilder;
        this.semanticGraphBuilder = semanticGraphBuilder == null ? new SemanticGraphBuilder() : semanticGraphBuilder;
    }

    public MappedUiKnowledge enrich(
            PageModelBundle pageModelBundle,
            MappedUiKnowledge mappedUiKnowledge,
            RuntimeEvidenceBundle runtimeEvidenceBundle
    ) {
        if (mappedUiKnowledge == null || pageModelBundle == null) {
            return mappedUiKnowledge;
        }
        if (mappedUiKnowledge.pages().isEmpty()) {
            return mappedUiKnowledge;
        }
        SemanticActionModel semanticActionModel = semanticActionModelBuilder.build(pageModelBundle, mappedUiKnowledge);
        SemanticGraphModel semanticGraph = semanticGraphBuilder.build(
                pageModelBundle,
                mappedUiKnowledge,
                semanticActionModel,
                runtimeEvidenceBundle
        );
        List<PageKnowledgeGraphNode> nodes = new ArrayList<>(mappedUiKnowledge.graphNodes());
        nodes.addAll(semanticGraph.nodes().stream().map(this::toGraphNode).toList());
        List<PageKnowledgeGraphEdge> edges = new ArrayList<>(mappedUiKnowledge.graphEdges());
        edges.addAll(semanticGraph.edges().stream().map(this::toGraphEdge).toList());
        List<PageKnowledgeVectorDocument> documents = new ArrayList<>(mappedUiKnowledge.vectorDocuments());
        documents.addAll(semanticGraph.nodes().stream()
                .filter(this::isVectorWorthy)
                .map(this::toVectorDocument)
                .toList());
        return new MappedUiKnowledge(
                mappedUiKnowledge.pages(),
                mappedUiKnowledge.transitions(),
                nodes,
                edges,
                documents
        );
    }

    private PageKnowledgeGraphNode toGraphNode(SemanticGraphNode node) {
        return new PageKnowledgeGraphNode(
                node.nodeId(),
                node.nodeType(),
                node.name(),
                node.pageId(),
                node.metadata()
        );
    }

    private PageKnowledgeGraphEdge toGraphEdge(SemanticGraphEdge edge) {
        return new PageKnowledgeGraphEdge(edge.fromId(), edge.toId(), edge.edgeType());
    }

    private boolean isVectorWorthy(SemanticGraphNode node) {
        String type = node.nodeType();
        return "SemanticPage".equals(type)
                || "SemanticActionCandidate".equals(type)
                || "SemanticElementActionCandidate".equals(type)
                || "BusinessIntentCandidate".equals(type)
                || "SemanticComponent".equals(type)
                || "RuntimeNetworkEvidence".equals(type)
                || "RuntimeStateTransition".equals(type);
    }

    private PageKnowledgeVectorDocument toVectorDocument(SemanticGraphNode node) {
        String text = node.nodeType() + " " + node.name()
                + " pageId=" + node.pageId()
                + " " + node.metadata().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(" "));
        return new PageKnowledgeVectorDocument(
                node.nodeId() + ":semantic-summary",
                "semantic-graph-summary",
                node.pageId(),
                node.nodeId(),
                text,
                keywords(node.nodeType(), node.name(), node.pageId(), text),
                java.util.Map.of("source", "semantic-graph")
        );
    }

    private List<String> keywords(String... parts) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            for (String token : part.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (token.length() >= 3) {
                    keywords.add(token);
                }
            }
        }
        return List.copyOf(keywords);
    }
}
