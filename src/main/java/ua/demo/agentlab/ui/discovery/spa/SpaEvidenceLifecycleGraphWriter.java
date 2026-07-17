package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceLifecycleResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Atomically promotes/demotes only evidence verified for the current requirement scope. */
public class SpaEvidenceLifecycleGraphWriter {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;

    public SpaEvidenceLifecycleGraphWriter(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public SpaEvidenceLifecycleGraphWriter(Neo4jRuntimeConfig config, JsonHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public SpaEvidenceLifecycleResult update(
            SpaTargetedVerificationResult verification,
            SpaInventoryConfig lifecycleConfig
    ) {
        if (verification == null || (verification.locatorVerifications().isEmpty() && verification.actionVerifications().isEmpty())) {
            return SpaEvidenceLifecycleResult.skipped("No targeted SPA evidence to update");
        }
        if (config == null || !config.enabled()) {
            return SpaEvidenceLifecycleResult.skipped("Neo4j persistence is disabled");
        }
        if (config.password() == null || config.password().isBlank()) {
            return SpaEvidenceLifecycleResult.skipped("Neo4j password is not configured");
        }
        if (httpClient == null) {
            return SpaEvidenceLifecycleResult.skipped("Neo4j HTTP client is unavailable");
        }
        SpaInventoryConfig effective = lifecycleConfig == null
                ? new SpaInventoryConfig(true, SpaDiscoveryMode.INVENTORY, 30, true, 0.80d, 2, 2,
                false, false, false, 14, 30, false)
                : lifecycleConfig;
        KnowledgeRunMetadata metadata = verification.runMetadata();
        if (metadata == null) {
            return SpaEvidenceLifecycleResult.skipped("SPA verification metadata is missing");
        }
        try {
            List<Map<String, Object>> statements = new ArrayList<>();
            if (!verification.locatorVerifications().isEmpty()) {
                statements.add(Map.of("statement", locatorUpdate(), "parameters", Map.of(
                        "evidence", verification.locatorVerifications().stream().map(item -> locatorPayload(item, metadata)).toList(),
                        "promoteAfter", effective.promoteAfterSuccesses(),
                        "demoteAfter", effective.demoteAfterFailures(),
                        "minScore", effective.minConfirmedScore()
                )));
            }
            if (!verification.actionVerifications().isEmpty()) {
                statements.add(Map.of("statement", actionUpdate(), "parameters", Map.of(
                        "evidence", verification.actionVerifications().stream().map(item -> actionPayload(item, metadata)).toList(),
                        "promoteAfter", effective.promoteAfterSuccesses(),
                        "demoteAfter", effective.demoteAfterFailures(),
                        "minScore", effective.minConfirmedScore()
                )));
            }
            JsonNode response = httpClient.post(commitUrl(), Map.of("statements", statements), headers());
            int locatorSuccess = (int) verification.locatorVerifications().stream().filter(TargetedLocatorVerification::verified).count();
            int actionSuccess = (int) verification.actionVerifications().stream().filter(TargetedActionVerification::verified).count();
            LifecycleCounts counts = lifecycleCounts(response);
            return new SpaEvidenceLifecycleResult(
                    true,
                    locatorSuccess,
                    verification.locatorVerifications().size() - locatorSuccess,
                    actionSuccess,
                    verification.actionVerifications().size() - actionSuccess,
                    counts.confirmed(),
                    counts.degraded(),
                    "Targeted SPA evidence lifecycle updated in Neo4j; promotion requires "
                            + effective.promoteAfterSuccesses() + " successful verification(s)",
                    List.of("spa-lifecycle:neo4j", "promotion-threshold=" + effective.promoteAfterSuccesses(),
                            "demotion-threshold=" + effective.demoteAfterFailures())
            );
        } catch (RuntimeException exception) {
            return SpaEvidenceLifecycleResult.skipped("Neo4j SPA lifecycle update failed: " + concise(exception));
        }
    }

    private Map<String, Object> locatorPayload(TargetedLocatorVerification evidence, KnowledgeRunMetadata metadata) {
        Map<String, Object> payload = basePayload(metadata, evidence.pageId(), evidence.route(), evidence.pageFingerprintHash(),
                evidence.componentId(), evidence.verified(), evidence.reason(), evidence.requirementIds());
        payload.put("locatorId", evidence.locatorId());
        payload.put("elementId", evidence.elementId());
        payload.put("strategy", evidence.strategy());
        payload.put("value", evidence.value());
        payload.put("qualityScore", evidence.qualityScore());
        payload.put("sameOrigin", evidence.verified());
        payload.put("globalMatchCount", evidence.verified() ? 1 : 0);
        payload.put("componentMatchCount", evidence.verified() ? 1 : 0);
        payload.put("uniqueWithinComponent", evidence.verified());
        payload.put("lastSeen", Instant.now().toString());
        payload.put("evidenceType", "CANDIDATE_LOCATOR");
        return payload;
    }

    private Map<String, Object> actionPayload(TargetedActionVerification evidence, KnowledgeRunMetadata metadata) {
        Map<String, Object> payload = basePayload(metadata, evidence.pageId(), evidence.route(), evidence.pageFingerprintHash(),
                evidence.componentId(), evidence.verified(), evidence.reason(), evidence.requirementIds());
        payload.put("actionId", evidence.actionId());
        payload.put("intent", evidence.intent());
        payload.put("targetElementId", evidence.targetElementId());
        payload.put("confidence", evidence.confidence());
        return payload;
    }

    private Map<String, Object> basePayload(
            KnowledgeRunMetadata metadata,
            String pageId,
            String route,
            String pageFingerprintHash,
            String componentId,
            boolean verified,
            String reason,
            List<String> requirementIds
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appId", metadata.appId());
        payload.put("baseUrlHash", metadata.baseUrlHash());
        payload.put("runId", metadata.runId());
        payload.put("requirementSetHash", metadata.requirementSetHash());
        payload.put("discoverySessionId", metadata.discoverySessionId());
        payload.put("schemaVersion", metadata.schemaVersion());
        payload.put("sourceAgent", metadata.sourceAgent());
        payload.put("pageId", pageId);
        payload.put("route", route);
        payload.put("pageFingerprintHash", pageFingerprintHash);
        payload.put("componentId", componentId);
        payload.put("verified", verified);
        payload.put("reason", reason);
        payload.put("requirementIds", String.join("|", requirementIds));
        payload.put("verifiedAt", Instant.now().toString());
        return payload;
    }

    private String locatorUpdate() {
        return "UNWIND $evidence AS e "
                + "MERGE (l:SpaCandidateLocator {appId:e.appId, baseUrlHash:e.baseUrlHash, schemaVersion:e.schemaVersion, "
                + "pageId:e.pageId, locatorId:e.locatorId}) "
                + "ON CREATE SET l.verificationSuccesses=0, l.verificationFailures=0, l.status='CANDIDATE' "
                + "WITH l,e, CASE WHEN e.verified THEN coalesce(l.verificationSuccesses,0)+1 ELSE coalesce(l.verificationSuccesses,0) END AS successes, "
                + "CASE WHEN e.verified THEN coalesce(l.verificationFailures,0) ELSE coalesce(l.verificationFailures,0)+1 END AS failures "
                + "SET l += e, l.verificationSuccesses=successes, l.verificationFailures=failures, "
                + "l.runtimePassRate=toFloat(successes)/toFloat(successes+failures), l.flakyRate=toFloat(failures)/toFloat(successes+failures), "
                + "l.validationStatus=CASE WHEN e.verified THEN 'PASSED' ELSE 'FAILED' END, "
                + "l.status=CASE WHEN failures >= $demoteAfter THEN 'DEGRADED' "
                + "WHEN successes >= $promoteAfter AND e.qualityScore >= $minScore THEN 'STABLE' "
                + "WHEN e.verified THEN 'LIVE_VERIFIED' ELSE coalesce(l.status,'CANDIDATE') END "
                + "RETURN l.status AS status";
    }

    private String actionUpdate() {
        return "UNWIND $evidence AS e "
                + "MERGE (a:SpaCandidateAction {appId:e.appId, baseUrlHash:e.baseUrlHash, schemaVersion:e.schemaVersion, "
                + "pageId:e.pageId, actionId:e.actionId}) "
                + "ON CREATE SET a.verificationSuccesses=0, a.verificationFailures=0, a.status='CANDIDATE' "
                + "WITH a,e, CASE WHEN e.verified THEN coalesce(a.verificationSuccesses,0)+1 ELSE coalesce(a.verificationSuccesses,0) END AS successes, "
                + "CASE WHEN e.verified THEN coalesce(a.verificationFailures,0) ELSE coalesce(a.verificationFailures,0)+1 END AS failures "
                + "SET a += e, a.verificationSuccesses=successes, a.verificationFailures=failures, "
                + "a.runtimePassRate=toFloat(successes)/toFloat(successes+failures), a.flakyRate=toFloat(failures)/toFloat(successes+failures), "
                + "a.validationStatus=CASE WHEN e.verified THEN 'PASSED' ELSE 'FAILED' END, "
                + "a.status=CASE WHEN failures >= $demoteAfter THEN 'DEGRADED' "
                + "WHEN successes >= $promoteAfter AND e.confidence >= $minScore THEN 'STABLE' "
                + "WHEN e.verified THEN 'LIVE_VERIFIED' ELSE coalesce(a.status,'CANDIDATE') END "
                + "RETURN a.status AS status";
    }

    private LifecycleCounts lifecycleCounts(JsonNode response) {
        int confirmed = 0;
        int degraded = 0;
        if (response == null) {
            return new LifecycleCounts(confirmed, degraded);
        }
        JsonNode results = response.path("results");
        if (!results.isArray()) {
            return new LifecycleCounts(confirmed, degraded);
        }
        for (JsonNode result : results) {
            JsonNode data = result.path("data");
            if (!data.isArray()) {
                continue;
            }
            for (JsonNode item : data) {
                String status = item.path("row").path(0).asText("");
                if ("STABLE".equalsIgnoreCase(status) || "PROMPT_ALLOWED".equalsIgnoreCase(status)) {
                    confirmed++;
                } else if ("DEGRADED".equalsIgnoreCase(status)) {
                    degraded++;
                }
            }
        }
        return new LifecycleCounts(confirmed, degraded);
    }

    private record LifecycleCounts(int confirmed, int degraded) {
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
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.replaceAll("\\s+", " ").trim();
    }
}
