package ua.demo.agentlab.ai.pageenrichment.service;

import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeMetadataCodec;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheVersion;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PageModelEnrichedKnowledgeAssembler {

    public MappedUiKnowledge merge(MappedUiKnowledge base, List<PageModelEnrichmentRecord> enrichments) {
        if (base == null || enrichments == null || enrichments.isEmpty()) {
            return base;
        }
        List<PageKnowledgeGraphNode> nodes = new ArrayList<>(base.graphNodes());
        List<PageKnowledgeGraphEdge> edges = new ArrayList<>(base.graphEdges());
        List<PageKnowledgeVectorDocument> documents = new ArrayList<>(base.vectorDocuments());
        for (PageModelEnrichmentRecord enrichment : enrichments) {
            String nodeId = enrichment.pageId() + ":enrichment";
            nodes.add(new PageKnowledgeGraphNode(nodeId, "PageEnrichment", enrichment.pageName(), enrichment.pageId(), metadata(enrichment)));
            edges.add(new PageKnowledgeGraphEdge(enrichment.pageId(), nodeId, "PAGE_ENRICHED_BY"));
            documents.add(new PageKnowledgeVectorDocument(
                    enrichment.pageId() + ":enrichment",
                    "page-enrichment",
                    enrichment.pageId(),
                    nodeId,
                    documentText(enrichment),
                    keywords(enrichment)
            ));
        }
        return new MappedUiKnowledge(base.pages(), base.transitions(), nodes, edges, documents);
    }

    private Map<String, String> metadata(PageModelEnrichmentRecord record) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("pageName", record.pageName());
        metadata.put("enrichmentCacheVersion", PageKnowledgeCacheVersion.CURRENT);
        metadata.put("route", record.route());
        metadata.put("businessIntent", record.businessIntent());
        metadata.put("pageSummary", record.pageSummary());
        metadata.put("source", record.enrichmentSource());
        metadata.put("confidence", String.valueOf(record.confidenceScore()));
        metadata.put("requirements", String.join(" | ", record.requirementTraceability()));
        metadata.put("supportedActions", PageKnowledgeMetadataCodec.encodeList(record.supportedActions()));
        metadata.put("stableLocators", PageKnowledgeMetadataCodec.encodeList(record.stableLocators()));
        metadata.put("preconditions", PageKnowledgeMetadataCodec.encodeList(record.preconditions()));
        metadata.put("postconditions", PageKnowledgeMetadataCodec.encodeList(record.postconditions()));
        metadata.put("risks", PageKnowledgeMetadataCodec.encodeList(record.risks()));
        metadata.put("coverageGaps", PageKnowledgeMetadataCodec.encodeList(record.coverageGaps()));
        metadata.put("requirementTraceability", PageKnowledgeMetadataCodec.encodeList(record.requirementTraceability()));
        metadata.put("actionsByRequirement", PageKnowledgeMetadataCodec.encodeFacts(record.actionsByRequirement()));
        metadata.put("postconditionsByRequirement", PageKnowledgeMetadataCodec.encodeFacts(record.postconditionsByRequirement()));
        return metadata;
    }

    private String documentText(PageModelEnrichmentRecord record) {
        return "Page enrichment for " + record.pageName() + " route=" + record.route()
                + " intent=" + record.businessIntent()
                + " actions=" + String.join(", ", record.supportedActions())
                + " stableLocators=" + String.join(", ", record.stableLocators())
                + " postconditions=" + String.join(", ", record.postconditions())
                + " requirements=" + String.join(", ", record.requirementTraceability());
    }

    private List<String> keywords(PageModelEnrichmentRecord record) {
        Set<String> keywords = new LinkedHashSet<>();
        keywords.add("page-enrichment");
        keywords.add(record.pageId());
        keywords.add(record.pageName());
        keywords.add(record.route());
        keywords.addAll(record.supportedActions());
        return keywords.stream().filter(value -> value != null && !value.isBlank()).toList();
    }
}
