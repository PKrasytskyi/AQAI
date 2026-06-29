package ua.demo.agentlab.ai.pageenrichment.cache;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeNamespaceFilterBuilder;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRetrievalMode;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PageKnowledgeCacheQueryService {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;
    private final KnowledgeNamespaceFilterBuilder namespaceFilterBuilder = new KnowledgeNamespaceFilterBuilder();

    public PageKnowledgeCacheQueryService(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public PageKnowledgeCacheQueryService(Neo4jRuntimeConfig config, JsonHttpClient httpClient) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient cannot be null");
        }
        this.config = config;
        this.httpClient = httpClient;
    }

    public Optional<PageModelEnrichmentRecord> findCachedEnrichment(
            KnowledgeRunMetadata runMetadata,
            String pageId,
            String pageFingerprintHash
    ) {
        if (!config.enabled() || runMetadata == null || isBlank(pageId) || isBlank(pageFingerprintHash)) {
            return Optional.empty();
        }
        Map<String, String> stableCacheFilter = namespaceFilterBuilder.stablePageCache(
                runMetadata,
                pageId,
                pageFingerprintHash,
                PageKnowledgeCacheVersion.CURRENT
        );
        JsonNode response = httpClient.post(
                commitUrl(),
                Map.of(
                        "statements",
                        List.of(Map.of(
                                "statement", queryStatement(),
                                "parameters", stableCacheFilter
                        ))
                ),
                headers()
        );
        JsonNode data = response.path("results").path(0).path("data");
        if (!data.isArray() || data.isEmpty()) {
            return Optional.empty();
        }
        JsonNode row = data.get(0).path("row");
        if (!row.isArray() || row.isEmpty() || !row.get(0).isObject()) {
            return Optional.empty();
        }
        return Optional.of(toRecord(row.get(0)));
    }

    private PageModelEnrichmentRecord toRecord(JsonNode node) {
        Map<String, String> properties = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> properties.put(entry.getKey(), entry.getValue().asText("")));
        return new PageModelEnrichmentRecord(
                properties.getOrDefault("pageId", ""),
                properties.getOrDefault("pageName", properties.getOrDefault("name", "")),
                properties.getOrDefault("route", ""),
                properties.getOrDefault("businessIntent", ""),
                properties.getOrDefault("pageSummary", ""),
                PageKnowledgeMetadataCodec.decodeList(properties.get("supportedActions")),
                PageKnowledgeMetadataCodec.decodeList(properties.get("stableLocators")),
                PageKnowledgeMetadataCodec.decodeList(properties.get("preconditions")),
                PageKnowledgeMetadataCodec.decodeList(properties.get("postconditions")),
                PageKnowledgeMetadataCodec.decodeList(properties.get("risks")),
                PageKnowledgeMetadataCodec.decodeList(properties.get("coverageGaps")),
                PageKnowledgeMetadataCodec.decodeList(properties.get("requirementTraceability")),
                PageKnowledgeMetadataCodec.decodeFacts(properties.get("actionsByRequirement")),
                PageKnowledgeMetadataCodec.decodeFacts(properties.get("postconditionsByRequirement")),
                parseDouble(properties.get("confidence"), 0.0d),
                "db-cache"
        );
    }

    private String queryStatement() {
        return """
                MATCH (n:UiKnowledgeNode:PageEnrichment)
                WHERE n.appId = $appId
                  AND n.baseUrlHash = $baseUrlHash
                  AND n.schemaVersion = $schemaVersion
                  AND n.enrichmentCacheVersion = $enrichmentCacheVersion
                  AND n.pageId = $pageId
                  AND n.pageFingerprintHash = $pageFingerprintHash
                RETURN properties(n)
                ORDER BY coalesce(toFloat(n.confidence), 0.0) DESC, coalesce(n.createdAt, "") DESC
                LIMIT 1
                """;
    }

    public KnowledgeRetrievalMode retrievalMode() {
        return KnowledgeRetrievalMode.STABLE_PAGE_CACHE;
    }

    private double parseDouble(String value, double fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String commitUrl() {
        String base = config.httpUrl().endsWith("/")
                ? config.httpUrl().substring(0, config.httpUrl().length() - 1)
                : config.httpUrl();
        return base + "/db/" + config.database() + "/tx/commit";
    }

    private Map<String, String> headers() {
        return Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (config.username() + ":" + safePassword()).getBytes(StandardCharsets.UTF_8)
        ));
    }

    private String safePassword() {
        if (config.password() == null || config.password().isBlank()) {
            throw new IllegalStateException("Neo4j password is not configured");
        }
        return config.password().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
