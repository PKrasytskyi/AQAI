package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Promotes only exact, pre-existing SPA evidence nodes after a successful bound browser behavior. */
public final class StructuredBehaviorPromotionGraphWriter {
    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient http;

    public StructuredBehaviorPromotionGraphWriter(Neo4jRuntimeConfig config) { this(config, new JsonHttpClient()); }
    StructuredBehaviorPromotionGraphWriter(Neo4jRuntimeConfig config, JsonHttpClient http) { this.config = config; this.http = http; }

    public String update(SpaBehaviorExecutionBundle bundle, SpaInventoryConfig lifecycle) {
        if (bundle == null || bundle.results().isEmpty()) return "skipped:no-structured-behavior-results";
        if (config == null || !config.enabled() || config.password() == null || config.password().isBlank()) return "skipped:neo4j-disabled";
        if (bundle.runMetadata() == null) return "skipped:missing-run-metadata";
        try {
            List<Map<String, Object>> evidence = bundle.results().stream()
                    .filter(result -> "PASSED".equalsIgnoreCase(result.status()) || "FAILED".equalsIgnoreCase(result.status()))
                    .map(result -> payload(bundle, result)).toList();
            if (evidence.isEmpty()) return "skipped:no-browser-execution-evidence";
            JsonNode response = http.post(commitUrl(), Map.of("statements", List.of(
                    Map.of("statement", locatorQuery(), "parameters", Map.of("items", evidence, "promoteAfter", lifecycle.promoteAfterSuccesses(), "demoteAfter", lifecycle.demoteAfterFailures())),
                    Map.of("statement", actionQuery(), "parameters", Map.of("items", evidence, "promoteAfter", lifecycle.promoteAfterSuccesses(), "demoteAfter", lifecycle.demoteAfterFailures())),
                    Map.of("statement", flowQuery(), "parameters", Map.of("items", evidence, "promoteAfter", lifecycle.promoteAfterSuccesses(), "demoteAfter", lifecycle.demoteAfterFailures()))
            )), headers());
            return response == null ? "updated:neo4j-empty-response" : "updated:neo4j-exact-evidence";
        } catch (RuntimeException exception) {
            return "skipped:neo4j-update-failed:" + concise(exception);
        }
    }

    private Map<String, Object> payload(SpaBehaviorExecutionBundle bundle, SpaBehaviorExecutionResult result) {
        var metadata = bundle.runMetadata();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appId", metadata.appId());
        payload.put("baseUrlHash", metadata.baseUrlHash());
        payload.put("schemaVersion", metadata.schemaVersion());
        payload.put("pageId", result.pageId());
        payload.put("route", result.route());
        payload.put("flowId", result.flowId());
        payload.put("locatorIds", result.locatorIds());
        payload.put("actionIds", result.actionIds());
        payload.put("passed", result.passed());
        payload.put("status", result.status());
        payload.put("runId", metadata.runId());
        payload.put("verifiedAt", Instant.now().toString());
        payload.put("reason", String.join(" | ", result.reasons()));
        return payload;
    }

    private String locatorQuery() {
        return "UNWIND $items AS i UNWIND i.locatorIds AS locatorId MATCH (l:SpaCandidateLocator {appId:i.appId,baseUrlHash:i.baseUrlHash,schemaVersion:i.schemaVersion,pageId:i.pageId,locatorId:locatorId}) "
                + "WITH l,i,CASE WHEN i.passed THEN coalesce(l.verificationSuccesses,0)+1 ELSE coalesce(l.verificationSuccesses,0) END AS successes,CASE WHEN i.passed THEN coalesce(l.verificationFailures,0) ELSE coalesce(l.verificationFailures,0)+1 END AS failures "
                + "SET l.verificationSuccesses=successes,l.verificationFailures=failures,l.runtimePassRate=toFloat(successes)/toFloat(successes+failures),l.flakyRate=toFloat(failures)/toFloat(successes+failures),l.lastSuccessfulSmoke=CASE WHEN i.passed THEN i.verifiedAt ELSE l.lastSuccessfulSmoke END,l.validationStatus=i.status,l.status=CASE WHEN failures >= $demoteAfter THEN 'DEGRADED' WHEN successes >= $promoteAfter THEN 'STABLE' WHEN i.passed THEN 'LIVE_VERIFIED' ELSE coalesce(l.status,'CANDIDATE') END RETURN count(l)";
    }

    private String actionQuery() {
        return "UNWIND $items AS i UNWIND i.actionIds AS actionId MATCH (a:SpaCandidateAction {appId:i.appId,baseUrlHash:i.baseUrlHash,schemaVersion:i.schemaVersion,pageId:i.pageId,actionId:actionId}) "
                + "WITH a,i,CASE WHEN i.passed THEN coalesce(a.verificationSuccesses,0)+1 ELSE coalesce(a.verificationSuccesses,0) END AS successes,CASE WHEN i.passed THEN coalesce(a.verificationFailures,0) ELSE coalesce(a.verificationFailures,0)+1 END AS failures "
                + "SET a.verificationSuccesses=successes,a.verificationFailures=failures,a.runtimePassRate=toFloat(successes)/toFloat(successes+failures),a.flakyRate=toFloat(failures)/toFloat(successes+failures),a.lastSuccessfulSmoke=CASE WHEN i.passed THEN i.verifiedAt ELSE a.lastSuccessfulSmoke END,a.validationStatus=i.status,a.status=CASE WHEN failures >= $demoteAfter THEN 'DEGRADED' WHEN successes >= $promoteAfter THEN 'STABLE' WHEN i.passed THEN 'LIVE_VERIFIED' ELSE coalesce(a.status,'CANDIDATE') END RETURN count(a)";
    }

    private String flowQuery() {
        return "UNWIND $items AS i WITH i WHERE i.flowId <> '' MATCH (f:SpaTypedComponentFlow {appId:i.appId,baseUrlHash:i.baseUrlHash,schemaVersion:i.schemaVersion,pageId:i.pageId,flowId:i.flowId}) "
                + "WITH f,i,CASE WHEN i.passed THEN coalesce(f.verificationSuccesses,0)+1 ELSE coalesce(f.verificationSuccesses,0) END AS successes,CASE WHEN i.passed THEN coalesce(f.verificationFailures,0) ELSE coalesce(f.verificationFailures,0)+1 END AS failures "
                + "SET f.verificationSuccesses=successes,f.verificationFailures=failures,f.runtimePassRate=toFloat(successes)/toFloat(successes+failures),f.flakyRate=toFloat(failures)/toFloat(successes+failures),f.lastSuccessfulSmoke=CASE WHEN i.passed THEN i.verifiedAt ELSE f.lastSuccessfulSmoke END,f.validationStatus=i.status,f.status=CASE WHEN failures >= $demoteAfter THEN 'DEGRADED' WHEN successes >= $promoteAfter THEN 'STABLE' WHEN i.passed THEN 'LIVE_VERIFIED' ELSE coalesce(f.status,'CANDIDATE') END RETURN count(f)";
    }

    private String commitUrl() { String root=config.httpUrl().endsWith("/")?config.httpUrl().substring(0,config.httpUrl().length()-1):config.httpUrl(); return root+"/db/"+config.database()+"/tx/commit"; }
    private Map<String,String> headers() { return Map.of("Authorization", "Basic "+ Base64.getEncoder().encodeToString((config.username()+":"+config.password()).getBytes(StandardCharsets.UTF_8))); }
    private String concise(RuntimeException exception) { return exception.getMessage()==null?exception.getClass().getSimpleName():exception.getMessage().replaceAll("\\s+", " ").trim(); }
}
