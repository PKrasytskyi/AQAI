package ua.demo.agentlab.ui.discovery.interaction.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.interaction.model.InteractionEvidenceStatus;
import ua.demo.agentlab.ui.discovery.interaction.model.LocatorPromotionDecision;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceBundle;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes Top-3 canonical evidence as a Neo4j projection; graph lookup is not part of promotion decisions. */
public final class CanonicalInteractionGraphProjectionWriter {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient http;

    public CanonicalInteractionGraphProjectionWriter(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    CanonicalInteractionGraphProjectionWriter(Neo4jRuntimeConfig config, JsonHttpClient http) {
        this.config = config;
        this.http = http;
    }

    public InteractionGraphProjectionResult persist(
            CanonicalInteractionEvidenceBundle canonical,
            UiInteractionInventory inventory
    ) {
        if (canonical == null || canonical.promotionDecisions().isEmpty()) {
            return InteractionGraphProjectionResult.skipped("canonical interaction evidence is empty");
        }
        if (config == null || !config.enabled()) return InteractionGraphProjectionResult.skipped("Neo4j is disabled");
        if (config.password() == null || config.password().isBlank()) {
            return InteractionGraphProjectionResult.skipped("Neo4j password is not configured");
        }
        KnowledgeRunMetadata metadata = inventory == null ? null : inventory.pages().stream()
                .map(ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage::runMetadata)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (metadata == null) return InteractionGraphProjectionResult.skipped("knowledge namespace metadata is missing");
        try {
            List<Map<String, Object>> evidence = payload(canonical, metadata);
            JsonNode response = http.post(commitUrl(), Map.of("statements", List.of(
                    Map.of("statement", projectionQuery(), "parameters", Map.of("evidence", evidence)))), headers());
            if (response != null && response.path("errors").isArray() && !response.path("errors").isEmpty()) {
                return InteractionGraphProjectionResult.skipped("Neo4j projection rejected: "
                        + response.path("errors").get(0).path("message").asText("unknown Neo4j error"));
            }
            long actions = evidence.stream().map(item -> item.get("actionKey")).distinct().count();
            long locators = evidence.stream().map(item -> item.get("locatorEvidenceId")).distinct().count();
            return new InteractionGraphProjectionResult(true, (int) actions, (int) locators, evidence.size(),
                    "Canonical Top-3 interaction graph projection persisted",
                    List.of("projection-only", "schema=canonical-interaction-evidence.v1"));
        } catch (RuntimeException exception) {
            return InteractionGraphProjectionResult.skipped("Neo4j projection failed: " + concise(exception));
        }
    }

    private List<Map<String, Object>> payload(
            CanonicalInteractionEvidenceBundle canonical,
            KnowledgeRunMetadata metadata
    ) {
        Map<String, Integer> rankByAction = new LinkedHashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        canonical.promotionDecisions().stream()
                .filter(item -> item.status() != InteractionEvidenceStatus.REJECTED)
                .sorted(java.util.Comparator.comparing(
                                (LocatorPromotionDecision item) -> item.interaction().scopedInteraction().candidate().actionKey().value())
                        .thenComparing((LocatorPromotionDecision item) -> item.primary() ? 0 : item.standby() ? 1 : 2)
                        .thenComparingDouble(item -> -item.interaction().finalScore().finalScore()))
                .forEach(decision -> {
                    var scoped = decision.interaction().scopedInteraction();
                    var candidate = scoped.candidate();
                    int rank = rankByAction.merge(candidate.actionKey().value(), 1, Integer::sum);
                    if (rank > 3) return;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("appId", metadata.appId());
                    row.put("baseUrlHash", metadata.baseUrlHash());
                    row.put("schemaVersion", metadata.schemaVersion());
                    row.put("evidenceSchemaVersion", canonical.schemaVersion());
                    row.put("runId", metadata.runId());
                    row.put("requirementSetHash", metadata.requirementSetHash());
                    row.put("discoverySessionId", metadata.discoverySessionId());
                    row.put("pageId", candidate.pageId());
                    row.put("pageName", provenance(candidate.provenance(), "page-name:"));
                    row.put("capability", provenance(candidate.provenance(), "page-capability:"));
                    row.put("route", candidate.route());
                    row.put("pageFingerprintHash", provenance(candidate.provenance(), "page-fingerprint:"));
                    row.put("stateId", candidate.elementKey().stateId());
                    row.put("componentId", candidate.componentId());
                    row.put("elementKey", candidate.elementKey().value());
                    row.put("semanticRole", candidate.elementKey().semanticRole());
                    row.put("actionKey", candidate.actionKey().value());
                    row.put("action", candidate.action().name());
                    row.put("locatorEvidenceId", candidate.locatorEvidenceId().value());
                    row.put("strategy", candidate.strategy());
                    row.put("value", candidate.value());
                    row.put("sameOrigin", candidate.sameOrigin());
                    row.put("globalMatchCount", candidate.globalMatchCount());
                    row.put("componentMatchCount", candidate.componentMatchCount());
                    row.put("rank", rank);
                    row.put("primary", decision.primary());
                    row.put("standby", decision.standby());
                    row.put("status", decision.status().name());
                    row.put("score", decision.interaction().finalScore().finalScore());
                    row.put("scoreBreakdown", decision.interaction().finalScore().factors().toString());
                    boolean runtimePass = runtimePassed(decision);
                    row.put("runtimePass", runtimePass);
                    row.put("runtimePassRate", runtimePass ? 1.0d : 0.0d);
                    row.put("flakyRate", runtimePass ? 0.0d : 1.0d);
                    row.put("validationStatus", runtimePass ? "PASSED" : "FAILED");
                    row.put("evidenceType", decision.status() == InteractionEvidenceStatus.CONFIRMED
                            ? "CONFIRMED_LOCATOR" : "CANDIDATE_LOCATOR");
                    row.put("requirementIds", String.join("|", scoped.requirementIds()));
                    row.put("updatedAt", Instant.now().toString());
                    result.add(row);
                });
        return result;
    }

    private String projectionQuery() {
        return "UNWIND $evidence AS e "
                + "MERGE (s:UiState {appId:e.appId,baseUrlHash:e.baseUrlHash,schemaVersion:e.schemaVersion,stateId:e.stateId}) "
                + "SET s.pageId=e.pageId,s.pageName=e.pageName,s.capability=e.capability,s.route=e.route,"
                + "s.pageFingerprintHash=e.pageFingerprintHash,s.lastSeen=e.updatedAt "
                + "MERGE (c:UiComponent {appId:e.appId,baseUrlHash:e.baseUrlHash,schemaVersion:e.schemaVersion,componentId:e.componentId}) "
                + "MERGE (el:UiSemanticElement {appId:e.appId,baseUrlHash:e.baseUrlHash,schemaVersion:e.schemaVersion,elementKey:e.elementKey}) "
                + "SET el.semanticRole=e.semanticRole "
                + "MERGE (a:UiSemanticAction {appId:e.appId,baseUrlHash:e.baseUrlHash,schemaVersion:e.schemaVersion,actionKey:e.actionKey}) "
                + "SET a.action=e.action,a.requirementIds=e.requirementIds,a.status=e.status,"
                + "a.lastRunId=e.runId,a.lastSeen=e.updatedAt "
                + "MERGE (l:UiLocatorEvidence {appId:e.appId,baseUrlHash:e.baseUrlHash,schemaVersion:e.schemaVersion,locatorEvidenceId:e.locatorEvidenceId}) "
                + "SET l.evidenceSchemaVersion=e.evidenceSchemaVersion,l.strategy=e.strategy,l.value=e.value,l.status=e.status,l.score=e.score,l.qualityScore=e.score,"
                + "l.scoreBreakdown=e.scoreBreakdown,l.evidenceType=e.evidenceType,l.sameOrigin=e.sameOrigin,"
                + "l.globalMatchCount=e.globalMatchCount,l.componentMatchCount=e.componentMatchCount,"
                + "l.pageFingerprintHash=e.pageFingerprintHash,l.runtimePassRate=e.runtimePassRate,"
                + "l.flakyRate=e.flakyRate,l.validationStatus=e.validationStatus,l.lastSeen=e.updatedAt "
                + "MERGE (s)-[:HAS_COMPONENT]->(c) MERGE (c)-[:HAS_ELEMENT]->(el) MERGE (el)-[:SUPPORTS_ACTION]->(a) "
                + "MERGE (a)-[r:USES_LOCATOR]->(l) SET r.rank=e.rank,r.primary=e.primary,r.standby=e.standby,r.runtimePass=e.runtimePass,r.updatedAt=e.updatedAt "
                + "RETURN count(r)";
    }

    private String commitUrl() {
        String root = config.httpUrl().endsWith("/")
                ? config.httpUrl().substring(0, config.httpUrl().length() - 1) : config.httpUrl();
        return root + "/db/" + config.database() + "/tx/commit";
    }

    private boolean runtimePassed(LocatorPromotionDecision decision) {
        var verification = decision.interaction().verification();
        return verification.locatorVerified()
                && verification.actionVerified()
                && verification.stateTransitionVerified()
                && verification.postconditionVerified();
    }

    private String provenance(List<String> values, String prefix) {
        return values.stream().filter(value -> value.startsWith(prefix))
                .map(value -> value.substring(prefix.length())).findFirst().orElse("");
    }

    private Map<String, String> headers() {
        return Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.password()).getBytes(StandardCharsets.UTF_8)));
    }

    private String concise(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName()
                : message.replaceAll("\\s+", " ").trim();
    }
}
