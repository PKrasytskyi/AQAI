package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KnowledgeNamespaceEnricher {

    private final PageKnowledgeFingerprintCalculator fingerprintCalculator = new PageKnowledgeFingerprintCalculator();

    public MappedUiKnowledge enrich(MappedUiKnowledge knowledge, KnowledgeRunMetadata runMetadata) {
        if (knowledge == null || runMetadata == null) {
            return knowledge;
        }
        Map<String, String> metadata = runMetadata.asMap();
        Map<String, String> pageFingerprints = knowledge.pages().stream()
                .collect(Collectors.toMap(
                        MappedPage::pageId,
                        fingerprintCalculator::fingerprint,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        List<PageKnowledgeGraphNode> nodes = knowledge.graphNodes().stream()
                .map(node -> enrichNode(node, metadata, pageFingerprints, runMetadata.runId()))
                .toList();
        List<PageKnowledgeGraphEdge> edges = knowledge.graphEdges().stream()
                .map(edge -> new PageKnowledgeGraphEdge(
                        namespaced(edge.fromId(), runMetadata.runId()),
                        namespaced(edge.toId(), runMetadata.runId()),
                        edge.edgeType()
                ))
                .toList();
        List<PageKnowledgeVectorDocument> documents = knowledge.vectorDocuments().stream()
                .map(document -> enrichDocument(document, metadata, pageFingerprints))
                .toList();
        return new MappedUiKnowledge(knowledge.pages(), knowledge.transitions(), nodes, edges, documents);
    }

    private PageKnowledgeGraphNode enrichNode(
            PageKnowledgeGraphNode node,
            Map<String, String> runMetadata,
            Map<String, String> pageFingerprints,
            String runId
    ) {
        Map<String, String> metadata = new LinkedHashMap<>(runMetadata);
        metadata.putAll(node.metadata());
        metadata.put("originalNodeId", node.nodeId());
        metadata.put("pageFingerprintHash", pageFingerprints.getOrDefault(node.pageId(), ""));
        metadata.put("pageKnowledgeVersion", runMetadata.getOrDefault("schemaVersion", ""));
        return new PageKnowledgeGraphNode(
                namespaced(node.nodeId(), runId),
                node.nodeType(),
                node.name(),
                node.pageId(),
                metadata
        );
    }

    private PageKnowledgeVectorDocument enrichDocument(
            PageKnowledgeVectorDocument document,
            Map<String, String> runMetadata,
            Map<String, String> pageFingerprints
    ) {
        Map<String, String> metadata = new LinkedHashMap<>(runMetadata);
        metadata.putAll(document.metadata());
        metadata.put("pageFingerprintHash", pageFingerprints.getOrDefault(document.sourcePageId(), ""));
        metadata.put("pageKnowledgeVersion", runMetadata.getOrDefault("schemaVersion", ""));
        return new PageKnowledgeVectorDocument(
                document.documentId(),
                document.documentType(),
                document.sourcePageId(),
                document.sourceEntityId(),
                document.text(),
                document.keywords(),
                metadata
        );
    }

    private String namespaced(String id, String runId) {
        if (id == null || id.isBlank()) {
            return runId + "::unknown";
        }
        return id.startsWith(runId + "::") ? id : runId + "::" + id;
    }
}
