package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
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

        List<Map<String, Object>> stableLocators = stableLocatorPayloads(knowledge);
        if (!stableLocators.isEmpty()) {
            statements.add(Map.of(
                    "statement", buildStableLocatorMergeStatement(),
                    "parameters", Map.of("locators", stableLocators)
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
                stableLocators.size(),
                "Mapped UI knowledge graph persisted to Neo4j; stableLocators=" + stableLocators.size()
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

    private String buildStableLocatorMergeStatement() {
        return """
                UNWIND $locators AS locator
                MERGE (l:UiStableLocator {
                  appId: locator.appId,
                  baseUrlHash: locator.baseUrlHash,
                  schemaVersion: locator.schemaVersion,
                  pageId: locator.pageId,
                  pageFingerprintHash: locator.pageFingerprintHash,
                  locatorKey: locator.locatorKey
                })
                ON CREATE SET
                  l.createdAt = locator.createdAt,
                  l.qualityScore = locator.qualityScore,
                  l.runtimePassRate = locator.runtimePassRate,
                  l.flakyRate = locator.flakyRate
                SET
                  l.lastSeen = locator.createdAt,
                  l.runId = locator.runId,
                  l.requirementSetHash = locator.requirementSetHash,
                  l.discoverySessionId = locator.discoverySessionId,
                  l.sourceAgent = locator.sourceAgent,
                  l.locatorId = locator.locatorId,
                  l.pageName = locator.pageName,
                  l.route = locator.route,
                  l.elementId = locator.elementId,
                  l.elementName = locator.elementName,
                  l.strategy = locator.strategy,
                  l.value = locator.value,
                  l.role = locator.role,
                  l.visibleText = locator.visibleText,
                  l.href = locator.href,
                  l.originHost = locator.originHost,
                  l.sameOrigin = locator.sameOrigin,
                  l.uniqueOnPage = locator.uniqueOnPage,
                  l.stableAcrossRuns = locator.stableAcrossRuns,
                  l.evidenceType = locator.evidenceType,
                  l.sourceTrace = locator.sourceTrace,
                  l.risks = locator.risks,
                  l.status = CASE
                    WHEN coalesce(toFloat(l.qualityScore), 0.0) <= locator.qualityScore THEN 'ACTIVE'
                    ELSE coalesce(l.status, 'ACTIVE')
                  END,
                  l.qualityScore = CASE
                    WHEN coalesce(toFloat(l.qualityScore), 0.0) <= locator.qualityScore THEN locator.qualityScore
                    ELSE toFloat(l.qualityScore)
                  END,
                  l.runtimePassRate = CASE
                    WHEN coalesce(toFloat(l.runtimePassRate), 0.0) <= locator.runtimePassRate THEN locator.runtimePassRate
                    ELSE toFloat(l.runtimePassRate)
                  END,
                  l.flakyRate = CASE
                    WHEN coalesce(toFloat(l.flakyRate), 1.0) >= locator.flakyRate THEN locator.flakyRate
                    ELSE toFloat(l.flakyRate)
                  END
                """;
    }

    private List<Map<String, Object>> stableLocatorPayloads(MappedUiKnowledge knowledge) {
        List<Map<String, Object>> locators = new ArrayList<>();
        Map<String, String> runMetadata = runMetadata(knowledge);
        if (isBlank(runMetadata.get("appId"))
                || isBlank(runMetadata.get("baseUrlHash"))
                || isBlank(runMetadata.get("schemaVersion"))) {
            return List.of();
        }
        Map<String, String> pageFingerprints = pageFingerprints(knowledge);
        for (MappedPage page : knowledge.pages()) {
            String pageFingerprint = pageFingerprints.getOrDefault(page.pageId(), "");
            if (pageFingerprint.isBlank()) {
                continue;
            }
            for (MappedElement element : page.elements()) {
                for (LocatorCandidate locator : element.locatorCandidates()) {
                    if (locator.evidenceType() == LocatorEvidenceType.CONFIRMED_LOCATOR) {
                        locators.add(stableLocatorPayload(runMetadata, page, pageFingerprint, element.elementId(),
                                element.semanticName(), element.role(), element.text(), locator));
                    }
                }
            }
            for (MappedForm form : page.forms()) {
                for (MappedField field : form.fields()) {
                    for (LocatorCandidate locator : field.locatorCandidates()) {
                        if (locator.evidenceType() == LocatorEvidenceType.CONFIRMED_LOCATOR) {
                            locators.add(stableLocatorPayload(runMetadata, page, pageFingerprint, field.fieldId(),
                                    field.fieldName(), field.fieldType(), field.label(), locator));
                        }
                    }
                }
            }
        }
        return locators;
    }

    private Map<String, Object> stableLocatorPayload(
            Map<String, String> runMetadata,
            MappedPage page,
            String pageFingerprint,
            String elementId,
            String elementName,
            String role,
            String visibleText,
            LocatorCandidate locator
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.putAll(runMetadata);
        payload.put("pageId", page.pageId());
        payload.put("pageName", page.pageName());
        payload.put("route", page.urlPattern().isBlank() ? page.url() : page.urlPattern());
        payload.put("pageFingerprintHash", pageFingerprint);
        payload.put("locatorId", safe(elementId) + ":locator:" + sanitize(locator.strategy().wireName() + "-" + locator.value()));
        payload.put("locatorKey", sanitize(page.pageId() + "-" + elementName + "-"
                + locator.strategy().wireName() + "-" + locator.value()));
        payload.put("elementId", elementId);
        payload.put("elementName", elementName);
        payload.put("strategy", locator.strategy().wireName());
        payload.put("value", locator.value());
        payload.put("role", locator.elementRole().isBlank() ? role : locator.elementRole());
        payload.put("visibleText", locator.visibleText().isBlank() ? visibleText : locator.visibleText());
        payload.put("href", locator.href());
        payload.put("originHost", locator.originHost());
        payload.put("sameOrigin", String.valueOf(locator.sameOrigin()));
        payload.put("uniqueOnPage", String.valueOf(locator.uniqueOnPage()));
        payload.put("stableAcrossRuns", String.valueOf(locator.stableAcrossRuns()));
        payload.put("evidenceType", locator.evidenceType().name());
        payload.put("qualityScore", locator.stabilityScore());
        payload.put("runtimePassRate", 1.0d);
        payload.put("flakyRate", 0.0d);
        payload.put("sourceTrace", "neo4j-stable-locator:" + page.pageId() + ":" + elementId);
        payload.put("risks", String.join(",", locator.risks()));
        return payload;
    }

    private Map<String, String> runMetadata(MappedUiKnowledge knowledge) {
        return knowledge.graphNodes().stream()
                .map(PageKnowledgeGraphNode::metadata)
                .filter(metadata -> metadata.containsKey("appId") && metadata.containsKey("baseUrlHash"))
                .findFirst()
                .map(metadata -> {
                    Map<String, String> copy = new LinkedHashMap<>();
                    copy.put("runId", metadata.getOrDefault("runId", ""));
                    copy.put("appId", metadata.getOrDefault("appId", ""));
                    copy.put("baseUrlHash", metadata.getOrDefault("baseUrlHash", ""));
                    copy.put("requirementSetHash", metadata.getOrDefault("requirementSetHash", ""));
                    copy.put("discoverySessionId", metadata.getOrDefault("discoverySessionId", ""));
                    copy.put("schemaVersion", metadata.getOrDefault("schemaVersion", KnowledgeRunMetadata.CURRENT_SCHEMA_VERSION));
                    copy.put("createdAt", metadata.getOrDefault("createdAt", ""));
                    copy.put("sourceAgent", metadata.getOrDefault("sourceAgent", ""));
                    return Map.copyOf(copy);
                })
                .orElse(Map.of(
                        "schemaVersion", KnowledgeRunMetadata.CURRENT_SCHEMA_VERSION,
                        "createdAt", ""
                ));
    }

    private Map<String, String> pageFingerprints(MappedUiKnowledge knowledge) {
        Map<String, String> fingerprints = new LinkedHashMap<>();
        for (PageKnowledgeGraphNode node : knowledge.graphNodes()) {
            if (!node.pageId().isBlank() && node.metadata().containsKey("pageFingerprintHash")) {
                fingerprints.putIfAbsent(node.pageId(), node.metadata().get("pageFingerprintHash"));
            }
        }
        return Map.copyOf(fingerprints);
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String sanitize(String value) {
        String normalized = safe(value).toLowerCase().replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "value" : normalized;
    }
}
