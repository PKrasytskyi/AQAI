package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.catalog.ConfirmedRouteGuard;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MappedUiKnowledgeRouteFilter {

    public MappedUiKnowledge filter(MappedUiKnowledge knowledge, ConfirmedRouteGuard guard) {
        if (knowledge == null || guard == null || !guard.hasConfirmedPages()) {
            return knowledge;
        }

        List<MappedPage> pages = knowledge.pages().stream()
                .filter(page -> guard.isConfirmed(page.pageName(), page.urlPattern())
                        || guard.isConfirmed(page.pageName(), page.url()))
                .toList();
        Set<String> pageIds = pages.stream()
                .map(MappedPage::pageId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<MappedTransition> transitions = knowledge.transitions().stream()
                .filter(transition -> pageIds.contains(transition.fromPageId())
                        && pageIds.contains(transition.toPageId()))
                .toList();
        List<PageKnowledgeGraphNode> graphNodes = knowledge.graphNodes().stream()
                .filter(node -> pageIds.contains(node.pageId()))
                .toList();
        Set<String> graphNodeIds = graphNodes.stream()
                .map(PageKnowledgeGraphNode::nodeId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<PageKnowledgeGraphEdge> graphEdges = knowledge.graphEdges().stream()
                .filter(edge -> graphNodeIds.contains(edge.fromId()) && graphNodeIds.contains(edge.toId()))
                .toList();
        List<PageKnowledgeVectorDocument> vectorDocuments = knowledge.vectorDocuments().stream()
                .filter(document -> pageIds.contains(document.sourcePageId()))
                .toList();

        return new MappedUiKnowledge(pages, transitions, graphNodes, graphEdges, vectorDocuments);
    }
}
