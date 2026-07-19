package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentActionDependency;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentInteractionGraph;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Persists only deterministic action prerequisites; no LLM inference enters this graph. */
public class ComponentInteractionGraphWriter {
    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient http;

    public ComponentInteractionGraphWriter(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public ComponentInteractionGraphWriter(Neo4jRuntimeConfig config, JsonHttpClient http) {
        this.config = config;
        this.http = http;
    }

    public String persist(ComponentInteractionGraph graph) {
        if (graph == null || graph.dependencies().isEmpty()) return "skipped:no-component-action-dependencies";
        if (config == null || !config.enabled()) return "skipped:neo4j-disabled";
        if (config.password() == null || config.password().isBlank()) return "skipped:neo4j-password-missing";
        try {
            var metadata = graph.runMetadata();
            List<Map<String, Object>> edges = graph.dependencies().stream().map(edge -> Map.<String, Object>of(
                    "appId", metadata.appId(), "baseUrlHash", metadata.baseUrlHash(),
                    "schemaVersion", metadata.schemaVersion(), "pageId", edge.pageId(),
                    "fromActionId", edge.prerequisiteActionId(), "toActionId", edge.dependentActionId(),
                    "componentId", edge.componentId(), "condition", edge.condition(), "confidence", edge.confidence()
            )).toList();
            http.post(commitUrl(), Map.of("statements", List.of(Map.of("statement", statement(), "parameters", Map.of("edges", edges)))), headers());
            return "persisted:component-action-dependencies=" + edges.size();
        } catch (RuntimeException exception) {
            return "skipped:neo4j-component-graph-failed:" + concise(exception);
        }
    }

    private String statement() {
        return "UNWIND $edges AS e "
                + "MATCH (from:SpaCandidateAction {appId:e.appId, baseUrlHash:e.baseUrlHash, schemaVersion:e.schemaVersion, pageId:e.pageId, actionId:e.fromActionId}) "
                + "MATCH (to:SpaCandidateAction {appId:e.appId, baseUrlHash:e.baseUrlHash, schemaVersion:e.schemaVersion, pageId:e.pageId, actionId:e.toActionId}) "
                + "MERGE (from)-[r:REQUIRES_ACTION]->(to) "
                + "SET r.componentId=e.componentId, r.condition=e.condition, r.confidence=e.confidence";
    }

    private String commitUrl() {
        String root = config.httpUrl().endsWith("/") ? config.httpUrl().substring(0, config.httpUrl().length() - 1) : config.httpUrl();
        return root + "/db/" + config.database() + "/tx/commit";
    }

    private Map<String, String> headers() {
        return Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.password()).getBytes(StandardCharsets.UTF_8)));
    }

    private String concise(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage().replaceAll("\\s+", " ").trim();
    }
}
