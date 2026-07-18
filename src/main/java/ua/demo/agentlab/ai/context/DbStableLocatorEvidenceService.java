package ua.demo.agentlab.ai.context;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeFingerprintCalculator;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbStableLocatorEvidenceService {

    private static final double MIN_CONFIRMED_SCORE = 0.75d;

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;
    private final PageKnowledgeFingerprintCalculator fingerprintCalculator = new PageKnowledgeFingerprintCalculator();

    public DbStableLocatorEvidenceService(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    DbStableLocatorEvidenceService(Neo4jRuntimeConfig config, JsonHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public List<PromptLocatorEvidence> findStableLocators(KnowledgeRunMetadata runMetadata, MappedPage page) {
        if (config == null || !config.enabled() || runMetadata == null || page == null || page.pageId().isBlank()) {
            return List.of();
        }
        String pageFingerprintHash = fingerprintCalculator.fingerprint(page);
        if (pageFingerprintHash.isBlank()) {
            return List.of();
        }
        try {
            JsonNode response = httpClient.post(
                    commitUrl(),
                    Map.of(
                            "statements",
                            List.of(Map.of(
                                    "statement", queryStatement(),
                                    "parameters", Map.of(
                                            "appId", runMetadata.appId(),
                                            "baseUrlHash", runMetadata.baseUrlHash(),
                                            "schemaVersion", runMetadata.schemaVersion(),
                                            "pageId", page.pageId(),
                                            "pageFingerprintHash", pageFingerprintHash,
                                            "route", route(page),
                                            "limit", 16
                                    )
                            ))
                    ),
                    headers()
            );
            return toPromptLocators(response);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<PromptLocatorEvidence> toPromptLocators(JsonNode response) {
        JsonNode data = response.path("results").path(0).path("data");
        if (!data.isArray() || data.isEmpty()) {
            return List.of();
        }
        List<PromptLocatorEvidence> locators = new ArrayList<>();
        for (JsonNode item : data) {
            JsonNode row = item.path("row");
            if (!row.isArray() || row.isEmpty() || !row.get(0).isObject()) {
                continue;
            }
            Map<String, String> properties = properties(row.get(0));
            double score = parseDouble(properties.get("qualityScore"), 0.0d);
            double runtimePassRate = parseDouble(properties.get("runtimePassRate"), 0.0d);
            double flakyRate = parseDouble(properties.get("flakyRate"), 1.0d);
            String validationStatus = properties.getOrDefault("validationStatus", "");
            if (score < MIN_CONFIRMED_SCORE
                    || runtimePassRate < 0.90d
                    || flakyRate > 0.10d
                    || !"PASSED".equalsIgnoreCase(validationStatus)
                    || !"CONFIRMED_LOCATOR".equals(properties.getOrDefault("evidenceType", ""))
                    || !"true".equalsIgnoreCase(properties.getOrDefault("sameOrigin", "false"))) {
                continue;
            }
            locators.add(new PromptLocatorEvidence(
                    fieldHint(firstNonBlank(properties.get("elementName"), properties.get("visibleText"), properties.get("locatorId"))),
                    firstNonBlank(properties.get("elementName"), properties.get("visibleText"), properties.get("locatorId")),
                    properties.getOrDefault("strategy", ""),
                    properties.getOrDefault("value", ""),
                    properties.getOrDefault("role", ""),
                    properties.getOrDefault("visibleText", ""),
                    properties.getOrDefault("href", ""),
                    true,
                    score,
                    "",
                    "",
                    parseInt(properties.get("globalMatchCount"), 1),
                    parseInt(firstNonBlank(properties.get("scopedMatchCount"), properties.get("componentMatchCount")), 1),
                    true,
                    LocatorEvidenceType.CONFIRMED_LOCATOR,
                    List.of(
                            "db-page-id:" + properties.getOrDefault("pageId", ""),
                            "db-route:" + properties.getOrDefault("route", ""),
                            "db-stable-locator:" + properties.getOrDefault("locatorId", ""),
                            "db-last-seen:" + properties.getOrDefault("lastSeen", ""),
                            "db-component-id:" + properties.getOrDefault("componentId", ""),
                            "db-validation-status:" + validationStatus,
                            "db-runtime-pass-rate:" + runtimePassRate,
                            "db-flaky-rate:" + flakyRate,
                            "evidenceType:CONFIRMED_LOCATOR"
                    )
            ));
        }
        return List.copyOf(locators);
    }

    private Map<String, String> properties(JsonNode node) {
        Map<String, String> properties = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> properties.put(entry.getKey(), entry.getValue().asText("")));
        return properties;
    }

    private String queryStatement() {
        return """
                MATCH (s:UiState)-[:HAS_COMPONENT]->(c:UiComponent)-[:HAS_ELEMENT]->
                      (:UiSemanticElement)-[:SUPPORTS_ACTION]->(:UiSemanticAction)-[r:USES_LOCATOR]->(l:UiLocatorEvidence)
                WHERE s.appId = $appId
                  AND s.baseUrlHash = $baseUrlHash
                  AND s.schemaVersion = $schemaVersion
                  AND s.pageId = $pageId
                  AND s.route = $route
                  AND s.pageFingerprintHash = $pageFingerprintHash
                  AND l.status = 'CONFIRMED'
                  AND l.schemaVersion = $schemaVersion
                  AND l.evidenceType = 'CONFIRMED_LOCATOR'
                  AND coalesce(l.sameOrigin, false) = true
                  AND coalesce(l.validationStatus, '') = 'PASSED'
                  AND coalesce(toFloat(l.runtimePassRate), 0.0) >= 0.90
                  AND coalesce(toFloat(l.flakyRate), 1.0) <= 0.10
                  AND coalesce(r.primary, false) = true
                WITH properties(l) + {
                  pageId:s.pageId,
                  route:s.route,
                  componentId:c.componentId,
                  locatorId:l.locatorEvidenceId,
                  scopedMatchCount:l.componentMatchCount
                } AS locator
                RETURN locator
                ORDER BY coalesce(toFloat(locator.qualityScore), 0.0) DESC,
                         coalesce(toFloat(locator.runtimePassRate), 0.0) DESC,
                         coalesce(locator.lastSeen, locator.verifiedAt, '') DESC
                LIMIT $limit
                """;
    }

    private String route(MappedPage page) {
        if (page == null) {
            return "";
        }
        return firstNonBlank(page.urlPattern(), page.url());
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

    private double parseDouble(String value, double fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String fieldHint(String value) {
        String normalized = value == null ? "" : value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toLowerCase();
        if (normalized.isBlank()) {
            return "element";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            builder.append(parts[index].substring(0, 1).toUpperCase()).append(parts[index].substring(1));
        }
        return builder.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
