package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.model.SpaStateGraph;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Persists browser-confirmed SPA states and action transitions without promoting locator evidence. */
public class SpaStateGraphWriter {
    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient http;

    public SpaStateGraphWriter(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public SpaStateGraphWriter(Neo4jRuntimeConfig config, JsonHttpClient http) {
        this.config = config;
        this.http = http;
    }

    public String persist(SpaStateGraph graph) {
        if (graph == null || graph.states().isEmpty()) return "skipped:no-live-spa-states";
        if (config == null || !config.enabled()) return "skipped:neo4j-disabled";
        if (config.password() == null || config.password().isBlank()) return "skipped:neo4j-password-missing";
        if (http == null) return "skipped:neo4j-http-unavailable";
        try {
            List<Map<String, Object>> states = graph.states().stream().map(this::statePayload).toList();
            List<Map<String, Object>> transitions = graph.transitions().stream().map(this::transitionPayload).toList();
            List<Map<String, Object>> statements = new java.util.ArrayList<>();
            statements.add(Map.of("statement", stateMerge(), "parameters", Map.of("states", states)));
            if (!transitions.isEmpty()) {
                statements.add(Map.of("statement", transitionMerge(), "parameters", Map.of("transitions", transitions)));
            }
            http.post(commitUrl(), Map.of("statements", statements), headers());
            return "persisted:ui-states=" + states.size() + ",executes=" + transitions.size();
        } catch (RuntimeException exception) {
            return "skipped:neo4j-ui-state-graph-failed:" + concise(exception);
        }
    }

    private Map<String, Object> statePayload(UiStateSnapshot state) {
        var metadata = state.runMetadata();
        return Map.ofEntries(
                Map.entry("stateId", state.stateId()), Map.entry("pageId", state.pageId()), Map.entry("route", state.route()),
                Map.entry("stateFingerprint", state.stateFingerprint()), Map.entry("appId", metadata == null ? "" : metadata.appId()),
                Map.entry("baseUrlHash", metadata == null ? "" : metadata.baseUrlHash()), Map.entry("schemaVersion", metadata == null ? SpaStateGraph.SCHEMA_VERSION : metadata.schemaVersion()),
                Map.entry("runId", metadata == null ? "" : metadata.runId()), Map.entry("requirementSetHash", metadata == null ? "" : metadata.requirementSetHash()),
                Map.entry("discoverySessionId", metadata == null ? "" : metadata.discoverySessionId()), Map.entry("createdAt", metadata == null ? "" : metadata.createdAt()),
                Map.entry("sourceAgent", metadata == null ? "ui-live-spa-targeted-verification-agent" : metadata.sourceAgent()),
                Map.entry("visibleComponentIds", String.join("|", state.visibleComponentIds())), Map.entry("overlaySignatures", String.join("|", state.overlaySignatures())),
                Map.entry("menuSignatures", String.join("|", state.menuSignatures())), Map.entry("modalSignatures", String.join("|", state.modalSignatures())),
                Map.entry("loading", state.loading()), Map.entry("networkIdle", state.networkIdle()), Map.entry("authenticated", state.authenticated()),
                Map.entry("confidence", state.confidence()), Map.entry("sourceTrace", String.join("|", state.sourceTrace())));
    }

    private Map<String, Object> transitionPayload(UiStateTransition transition) {
        var metadata = transition.runMetadata();
        return Map.ofEntries(
                Map.entry("transitionId", transition.transitionId()), Map.entry("fromStateId", transition.fromStateId()),
                Map.entry("toStateId", transition.toStateId()), Map.entry("actionId", transition.actionId()),
                Map.entry("actionIntent", transition.actionIntent()), Map.entry("postcondition", transition.postcondition()),
                Map.entry("routeChanged", transition.routeChanged()), Map.entry("sameRouteStateChange", transition.sameRouteStateChange()),
                Map.entry("confidence", transition.confidence()), Map.entry("appId", metadata == null ? "" : metadata.appId()),
                Map.entry("baseUrlHash", metadata == null ? "" : metadata.baseUrlHash()), Map.entry("schemaVersion", metadata == null ? SpaStateGraph.SCHEMA_VERSION : metadata.schemaVersion()),
                Map.entry("runId", metadata == null ? "" : metadata.runId()), Map.entry("requirementSetHash", metadata == null ? "" : metadata.requirementSetHash()),
                Map.entry("discoverySessionId", metadata == null ? "" : metadata.discoverySessionId()),
                Map.entry("sourceTrace", String.join("|", transition.sourceTrace())));
    }

    private String stateMerge() {
        return "UNWIND $states AS s "
                + "MERGE (state:UiState {appId:s.appId, baseUrlHash:s.baseUrlHash, schemaVersion:s.schemaVersion, stateFingerprint:s.stateFingerprint}) "
                + "ON CREATE SET state.firstSeen=s.createdAt "
                + "SET state += s, state.lastSeen=s.createdAt";
    }

    private String transitionMerge() {
        return "UNWIND $transitions AS t "
                + "MATCH (from:UiState {appId:t.appId, baseUrlHash:t.baseUrlHash, schemaVersion:t.schemaVersion, stateId:t.fromStateId}) "
                + "MATCH (to:UiState {appId:t.appId, baseUrlHash:t.baseUrlHash, schemaVersion:t.schemaVersion, stateId:t.toStateId}) "
                + "MERGE (from)-[edge:EXECUTES {transitionId:t.transitionId, actionId:t.actionId}]->(to) "
                + "SET edge += t, edge.runtimePassRate=1.0, edge.flakyRate=0.0, edge.validationStatus='PASSED'";
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
