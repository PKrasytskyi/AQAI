package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphPageKnowledgeWriter implements PageKnowledgeWriter {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;

    public GraphPageKnowledgeWriter(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public GraphPageKnowledgeWriter(Neo4jRuntimeConfig config, JsonHttpClient httpClient) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient cannot be null");
        }
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public PageKnowledgeWriteResult write(MappedUiKnowledge knowledge) {
        if (!config.enabled()) {
            return new PageKnowledgeWriteResult("neo4j", false, 0, 0, 0, "Neo4j persistence is disabled");
        }
        if (knowledge == null) {
            throw new IllegalArgumentException("knowledge cannot be null");
        }

        List<Map<String, Object>> statements = new ArrayList<>();
        Map<String, List<PageKnowledgeGraphNode>> nodesByLabel = knowledge.graphNodes().stream()
                .collect(Collectors.groupingBy(node -> normalizeLabel(node.nodeType()), LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<PageKnowledgeGraphNode>> entry : nodesByLabel.entrySet()) {
            statements.add(Map.of(
                    "statement", buildNodeMergeStatement(entry.getKey()),
                    "parameters", Map.of("nodes", entry.getValue().stream().map(this::toNodePayload).toList())
            ));
        }

        Map<String, List<PageKnowledgeGraphEdge>> edgesByType = knowledge.graphEdges().stream()
                .collect(Collectors.groupingBy(edge -> normalizeRelationshipType(edge.edgeType()), LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<PageKnowledgeGraphEdge>> entry : edgesByType.entrySet()) {
            statements.add(Map.of(
                    "statement", buildEdgeMergeStatement(entry.getKey()),
                    "parameters", Map.of("edges", entry.getValue().stream().map(this::toEdgePayload).toList())
            ));
        }

        if (!statements.isEmpty()) {
            httpClient.post(
                    commitUrl(),
                    Map.of("statements", statements),
                    headers()
            );
        }

        return new PageKnowledgeWriteResult(
                "neo4j",
                true,
                knowledge.graphNodes().size(),
                knowledge.graphEdges().size(),
                0,
                "Mapped UI knowledge graph persisted to Neo4j"
        );
    }

    private Map<String, Object> toNodePayload(PageKnowledgeGraphNode node) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nodeId", node.nodeId());
        payload.put("nodeType", node.nodeType());
        payload.put("name", node.name());
        payload.put("pageId", node.pageId());
        payload.put("metadata", node.metadata());
        return payload;
    }

    private Map<String, Object> toEdgePayload(PageKnowledgeGraphEdge edge) {
        return Map.of(
                "fromId", edge.fromId(),
                "toId", edge.toId(),
                "edgeType", edge.edgeType()
        );
    }

    private String buildNodeMergeStatement(String label) {
        return "UNWIND $nodes AS node "
                + "MERGE (n:UiKnowledgeNode:" + label + " {nodeId: node.nodeId}) "
                + "SET n.name = node.name, "
                + "n.nodeType = node.nodeType, "
                + "n.pageId = node.pageId "
                + "SET n += node.metadata";
    }

    private String buildEdgeMergeStatement(String relationshipType) {
        return "UNWIND $edges AS edge "
                + "MATCH (source:UiKnowledgeNode {nodeId: edge.fromId}) "
                + "MATCH (target:UiKnowledgeNode {nodeId: edge.toId}) "
                + "MERGE (source)-[rel:" + relationshipType + "]->(target) "
                + "SET rel.edgeType = edge.edgeType";
    }

    private String commitUrl() {
        return trimTrailingSlash(config.httpUrl()) + "/db/" + config.database() + "/tx/commit";
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (config.username() + ":" + safePassword()).getBytes(StandardCharsets.UTF_8)));
        return headers;
    }

    private String safePassword() {
        if (config.password() == null || config.password().isBlank()) {
            throw new IllegalStateException("Neo4j password is not configured");
        }
        return config.password();
    }

    private String normalizeLabel(String label) {
        String normalized = safe(label).replaceAll("[^A-Za-z0-9_]", "");
        return normalized.isBlank() ? "UiKnowledgeNode" : normalized;
    }

    private String normalizeRelationshipType(String edgeType) {
        String normalized = safe(edgeType).toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        return normalized.isBlank() ? "RELATED_TO" : normalized;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
