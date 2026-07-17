package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceRetentionResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Retires degraded or unseen SPA candidates. Hard deletion is opt-in: retained nodes preserve
 * auditability and cannot be selected for POM evidence after their status becomes RETIRED.
 */
public class SpaEvidenceRetentionGraphWriter {
    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient http;

    public SpaEvidenceRetentionGraphWriter(Neo4jRuntimeConfig config) { this(config, new JsonHttpClient()); }
    public SpaEvidenceRetentionGraphWriter(Neo4jRuntimeConfig config, JsonHttpClient http) { this.config = config; this.http = http; }

    public SpaEvidenceRetentionResult apply(KnowledgeRunMetadata metadata, SpaInventoryConfig policy) {
        if (metadata == null || policy == null || !policy.retentionEnabled()) return SpaEvidenceRetentionResult.skipped("SPA retention is disabled or metadata is missing");
        if (config == null || !config.enabled()) return SpaEvidenceRetentionResult.skipped("Neo4j persistence is disabled");
        if (config.password() == null || config.password().isBlank()) return SpaEvidenceRetentionResult.skipped("Neo4j password is not configured");
        try {
            String now = Instant.now().toString();
            String degradedBefore = Instant.now().minus(policy.degradedRetentionDays(), ChronoUnit.DAYS).toString();
            String orphanBefore = Instant.now().minus(policy.orphanRetentionDays(), ChronoUnit.DAYS).toString();
            JsonNode response = http.post(commitUrl(), Map.of("statements", List.of(Map.of("statement", statement(policy.retentionHardDelete()),
                    "parameters", Map.of("appId", metadata.appId(), "baseUrlHash", metadata.baseUrlHash(),
                            "schemaVersion", metadata.schemaVersion(), "degradedBefore", degradedBefore,
                            "orphanBefore", orphanBefore, "now", now)))), headers());
            int degraded = read(response, 0);
            int orphan = read(response, 1);
            int deleted = read(response, 2);
            return new SpaEvidenceRetentionResult(true, degraded, orphan, deleted,
                    "SPA evidence retention applied (hardDelete=" + policy.retentionHardDelete() + ")",
                    List.of("degraded-before=" + degradedBefore, "orphan-before=" + orphanBefore));
        } catch (RuntimeException exception) {
            return SpaEvidenceRetentionResult.skipped("Neo4j SPA retention failed: " + concise(exception));
        }
    }

    private String statement(boolean hardDelete) {
        if (hardDelete) {
            return "MATCH (n) WHERE (n:SpaCandidateLocator OR n:SpaCandidateAction) AND n.appId=$appId AND n.baseUrlHash=$baseUrlHash AND n.schemaVersion=$schemaVersion "
                    + "WITH collect(n) AS candidates, $degradedBefore AS degradedBefore, $orphanBefore AS orphanBefore "
                    + "FOREACH (n IN [x IN candidates WHERE (x.status='DEGRADED' AND coalesce(x.verifiedAt,x.lastSeen,x.createdAt,'') < degradedBefore) OR coalesce(x.lastSeen,x.createdAt,'') < orphanBefore] | DETACH DELETE n) "
                    + "RETURN 0 AS degradedRetired, 0 AS orphanRetired, size(candidates) AS deleted";
        }
        return "MATCH (n) WHERE (n:SpaCandidateLocator OR n:SpaCandidateAction) AND n.appId=$appId AND n.baseUrlHash=$baseUrlHash AND n.schemaVersion=$schemaVersion "
                + "WITH collect(n) AS candidates, $degradedBefore AS degradedBefore, $orphanBefore AS orphanBefore, $now AS now "
                + "FOREACH (n IN [x IN candidates WHERE x.status='DEGRADED' AND coalesce(x.verifiedAt,x.lastSeen,x.createdAt,'') < degradedBefore] | SET n.status='RETIRED', n.retentionReason='degraded-evidence-expired', n.retiredAt=now) "
                + "FOREACH (n IN [x IN candidates WHERE coalesce(x.lastSeen,x.createdAt,'') < orphanBefore AND NOT (coalesce(x.status,'CANDIDATE') IN ['STABLE','PROMPT_ALLOWED'])] | SET n.status='RETIRED', n.retentionReason='orphan-candidate-expired', n.retiredAt=now) "
                + "RETURN size([x IN candidates WHERE x.status='RETIRED' AND x.retentionReason='degraded-evidence-expired']) AS degradedRetired, "
                + "size([x IN candidates WHERE x.status='RETIRED' AND x.retentionReason='orphan-candidate-expired']) AS orphanRetired, 0 AS deleted";
    }

    private int read(JsonNode response, int index) {
        return response == null ? 0 : response.path("results").path(0).path("data").path(0).path("row").path(index).asInt(0);
    }
    private String commitUrl() { String root = config.httpUrl().endsWith("/") ? config.httpUrl().substring(0, config.httpUrl().length()-1) : config.httpUrl(); return root + "/db/" + config.database() + "/tx/commit"; }
    private Map<String,String> headers() { return Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString((config.username()+":"+config.password()).getBytes(StandardCharsets.UTF_8))); }
    private String concise(RuntimeException e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().replaceAll("\\s+", " ").trim(); }
}
