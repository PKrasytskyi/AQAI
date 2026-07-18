package ua.demo.agentlab.ai.context;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Neo4jUiKnowledgeQueryService {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;

    public Neo4jUiKnowledgeQueryService(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public Neo4jUiKnowledgeQueryService(Neo4jRuntimeConfig config, JsonHttpClient httpClient) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient cannot be null");
        }
        this.config = config;
        this.httpClient = httpClient;
    }

    public List<UiKnowledgeGraphMatch> search(
            List<String> seedNodeIds,
            List<String> pageIds,
            List<String> queryTerms,
            int limit
    ) {
        return search(seedNodeIds, pageIds, queryTerms, limit, Map.of());
    }

    public List<UiKnowledgeGraphMatch> search(
            List<String> seedNodeIds,
            List<String> pageIds,
            List<String> queryTerms,
            int limit,
            Map<String, String> namespaceFilter
    ) {
        if (!config.enabled()) {
            return List.of();
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("seedIds", seedNodeIds == null ? List.of() : seedNodeIds);
        parameters.put("pageIds", pageIds == null ? List.of() : pageIds);
        parameters.put("terms", queryTerms == null ? List.of() : queryTerms);
        parameters.put("limit", Math.max(limit, 1));
        parameters.put("runId", namespaceValue(namespaceFilter, "runId"));
        parameters.put("appId", namespaceValue(namespaceFilter, "appId"));
        parameters.put("baseUrlHash", namespaceValue(namespaceFilter, "baseUrlHash"));
        parameters.put("requirementSetHash", namespaceValue(namespaceFilter, "requirementSetHash"));
        parameters.put("discoverySessionId", namespaceValue(namespaceFilter, "discoverySessionId"));
        parameters.put("schemaVersion", namespaceValue(namespaceFilter, "schemaVersion"));
        JsonNode response = httpClient.post(
                commitUrl(),
                Map.of(
                        "statements",
                        List.of(Map.of(
                                "statement", queryStatement(),
                                "parameters", parameters
                        ))
                ),
                headers()
        );

        List<UiKnowledgeGraphMatch> matches = new ArrayList<>();
        JsonNode data = response.path("results").path(0).path("data");
        if (!data.isArray()) {
            return matches;
        }
        data.forEach(item -> {
            JsonNode row = item.path("row");
            if (!row.isArray() || row.size() < 7) {
                return;
            }
            Map<String, String> metadata = new LinkedHashMap<>();
            JsonNode metadataNode = row.get(6);
            if (metadataNode != null && metadataNode.isObject()) {
                metadataNode.properties().forEach(entry -> metadata.put(entry.getKey(), entry.getValue().asText("")));
            }
            matches.add(new UiKnowledgeGraphMatch(
                    row.get(0).asText(""),
                    row.get(1).asText(""),
                    row.get(2).asText(""),
                    row.get(3).asText(""),
                    row.get(4).asText(""),
                    row.get(5).asDouble(0.0d),
                    metadata
            ));
        });
        return matches;
    }

    private String queryStatement() {
        return """
                MATCH (n:UiKnowledgeNode)
                WHERE n.runId = $runId
                  AND n.appId = $appId
                  AND n.baseUrlHash = $baseUrlHash
                  AND n.requirementSetHash = $requirementSetHash
                  AND n.discoverySessionId = $discoverySessionId
                  AND n.schemaVersion = $schemaVersion
                  AND (
                    (size($seedIds) = 0 AND size($pageIds) = 0)
                    OR n.nodeId IN $seedIds
                    OR n.originalNodeId IN $seedIds
                    OR n.pageId IN $pageIds
                )
                WITH DISTINCT n, $terms AS terms, $pageIds AS pageIds, $seedIds AS seedIds
                WHERE size(terms) = 0
                    OR any(term IN terms
                        WHERE toLower(coalesce(n.name, "")) CONTAINS term
                           OR toLower(coalesce(n.nodeType, "")) CONTAINS term
                           OR toLower(coalesce(n.pageId, "")) CONTAINS term)
                OPTIONAL MATCH (n)-[rel]-(neighbor:UiKnowledgeNode)
                WHERE neighbor IS NULL
                   OR (neighbor.runId = $runId
                       AND neighbor.appId = $appId
                       AND neighbor.baseUrlHash = $baseUrlHash
                       AND neighbor.requirementSetHash = $requirementSetHash
                       AND neighbor.discoverySessionId = $discoverySessionId
                       AND neighbor.schemaVersion = $schemaVersion)
                WITH n, rel, pageIds, terms, seedIds,
                     CASE WHEN n.nodeId IN seedIds THEN 1.2 ELSE 0.0 END
                     + CASE WHEN n.originalNodeId IN seedIds THEN 1.2 ELSE 0.0 END
                     + CASE WHEN n.pageId IN pageIds THEN 0.8 ELSE 0.0 END
                     + reduce(score = 0.0, term IN terms |
                         score
                         + CASE WHEN toLower(coalesce(n.name, "")) CONTAINS term THEN 0.45 ELSE 0.0 END
                         + CASE WHEN toLower(coalesce(n.nodeType, "")) CONTAINS term THEN 0.20 ELSE 0.0 END
                     ) AS relevance
                RETURN n.nodeId,
                       n.nodeType,
                       n.name,
                       n.pageId,
                       coalesce(type(rel), ""),
                       relevance,
                       properties(n)
                ORDER BY relevance DESC
                LIMIT $limit
                """;
    }

    private String namespaceValue(Map<String, String> namespaceFilter, String key) {
        return namespaceFilter == null ? "" : namespaceFilter.getOrDefault(key, "");
    }

    private String commitUrl() {
        String base = config.httpUrl().endsWith("/")
                ? config.httpUrl().substring(0, config.httpUrl().length() - 1)
                : config.httpUrl();
        return base + "/db/" + config.database() + "/tx/commit";
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (config.username() + ":" + safePassword()).getBytes(StandardCharsets.UTF_8)
        ));
        return headers;
    }

    private String safePassword() {
        if (config.password() == null || config.password().isBlank()) {
            throw new IllegalStateException("Neo4j password is not configured");
        }
        return config.password().trim();
    }
}
