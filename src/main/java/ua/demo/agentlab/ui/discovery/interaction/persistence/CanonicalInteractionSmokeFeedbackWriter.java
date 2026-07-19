package ua.demo.agentlab.ui.discovery.interaction.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.generated.PomSourceMap;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaSmokeEvidenceFeedbackResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies generated-code smoke feedback to the canonical interaction graph. */
public final class CanonicalInteractionSmokeFeedbackWriter {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient http;

    public CanonicalInteractionSmokeFeedbackWriter(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    CanonicalInteractionSmokeFeedbackWriter(Neo4jRuntimeConfig config, JsonHttpClient http) {
        this.config = config;
        this.http = http;
    }

    public SpaSmokeEvidenceFeedbackResult write(
            boolean liveSmokePassed,
            PomSourceMap sourceMap,
            UiInteractionInventory inventory,
            SpaTargetedVerificationResult verification,
            SpaInventoryConfig policy
    ) {
        if (verification == null || sourceMap == null || sourceMap.entries().isEmpty()) {
            return SpaSmokeEvidenceFeedbackResult.skipped("POM source map or live verification evidence is missing");
        }
        if (config == null || !config.enabled()) {
            return SpaSmokeEvidenceFeedbackResult.skipped("Neo4j persistence is disabled");
        }
        if (config.password() == null || config.password().isBlank()) {
            return SpaSmokeEvidenceFeedbackResult.skipped("Neo4j password is not configured");
        }
        List<Map<String, Object>> evidence = feedbackRows(sourceMap, verification, liveSmokePassed);
        if (evidence.isEmpty()) {
            return SpaSmokeEvidenceFeedbackResult.skipped(
                    "No browser-verified canonical locator is referenced by the generated POM source map");
        }
        try {
            JsonNode response = http.post(commitUrl(), Map.of("statements", List.of(Map.of(
                    "statement", feedbackStatement(),
                    "parameters", Map.of(
                            "evidence", evidence,
                            "promoteAfter", policy.promoteAfterSuccesses(),
                            "demoteAfter", policy.demoteAfterFailures()
                    )
            ))), headers());
            if (response != null && response.path("errors").isArray() && !response.path("errors").isEmpty()) {
                return SpaSmokeEvidenceFeedbackResult.skipped("Neo4j canonical smoke feedback rejected: "
                        + response.path("errors").get(0).path("message").asText("unknown Neo4j error"));
            }
            return new SpaSmokeEvidenceFeedbackResult(
                    true,
                    liveSmokePassed,
                    evidence.size(),
                    evidence.size(),
                    "Generated POM smoke feedback updated canonical interaction evidence",
                    List.of("graph=canonical", "live-smoke=" + liveSmokePassed)
            );
        } catch (RuntimeException exception) {
            return SpaSmokeEvidenceFeedbackResult.skipped("Neo4j canonical smoke feedback failed: " + concise(exception));
        }
    }

    private List<Map<String, Object>> feedbackRows(
            PomSourceMap sourceMap,
            SpaTargetedVerificationResult verification,
            boolean passed
    ) {
        Map<String, PomSourceMap.PageEntry> pagesByRoute = new LinkedHashMap<>();
        sourceMap.entries().forEach(page -> pagesByRoute.put(page.route(), page));
        String now = Instant.now().toString();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TargetedLocatorVerification locator : verification.locatorVerifications()) {
            if (!locator.verified()) continue;
            PomSourceMap.PageEntry page = pagesByRoute.get(locator.route());
            if (page == null || page.fields().stream().noneMatch(field ->
                    field.strategy().equalsIgnoreCase(locator.strategy()) && field.value().equals(locator.value()))) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("appId", verification.runMetadata().appId());
            row.put("baseUrlHash", verification.runMetadata().baseUrlHash());
            row.put("schemaVersion", verification.runMetadata().schemaVersion());
            row.put("pageId", locator.pageId());
            row.put("route", locator.route());
            row.put("strategy", locator.strategy());
            row.put("value", locator.value());
            row.put("passed", passed);
            row.put("at", now);
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private String feedbackStatement() {
        return "UNWIND $evidence AS e "
                + "MATCH (s:UiState)-[:HAS_COMPONENT]->(:UiComponent)-[:HAS_ELEMENT]->(:UiSemanticElement)"
                + "-[:SUPPORTS_ACTION]->(a:UiSemanticAction)-[:USES_LOCATOR]->(l:UiLocatorEvidence) "
                + "WHERE s.appId=e.appId AND s.baseUrlHash=e.baseUrlHash AND s.schemaVersion=e.schemaVersion "
                + "AND s.pageId=e.pageId AND s.route=e.route AND l.schemaVersion=e.schemaVersion "
                + "AND toLower(l.strategy)=toLower(e.strategy) AND l.value=e.value "
                + "WITH DISTINCT l,a,e,CASE WHEN e.passed THEN coalesce(l.smokeSuccesses,0)+1 ELSE coalesce(l.smokeSuccesses,0) END AS successes,"
                + "CASE WHEN e.passed THEN coalesce(l.smokeFailures,0) ELSE coalesce(l.smokeFailures,0)+1 END AS failures "
                + "SET l.smokeSuccesses=successes,l.smokeFailures=failures,l.lastSmokeStatus=CASE WHEN e.passed THEN 'PASSED' ELSE 'FAILED' END,"
                + "l.lastSuccessfulSmoke=CASE WHEN e.passed THEN e.at ELSE coalesce(l.lastSuccessfulSmoke,'') END,"
                + "l.runtimePassRate=toFloat(successes)/toFloat(CASE WHEN successes+failures=0 THEN 1 ELSE successes+failures END),"
                + "l.flakyRate=toFloat(failures)/toFloat(CASE WHEN successes+failures=0 THEN 1 ELSE successes+failures END),"
                + "l.validationStatus=CASE WHEN e.passed THEN 'PASSED' ELSE 'FAILED' END,"
                + "l.status=CASE WHEN failures >= $demoteAfter THEN 'DEGRADED' "
                + "WHEN e.passed AND successes >= $promoteAfter AND coalesce(l.qualityScore,0)>=0.75 THEN 'CONFIRMED' ELSE l.status END,"
                + "l.evidenceType=CASE WHEN failures >= $demoteAfter THEN 'CANDIDATE_LOCATOR' "
                + "WHEN e.passed AND successes >= $promoteAfter AND coalesce(l.qualityScore,0)>=0.75 THEN 'CONFIRMED_LOCATOR' ELSE l.evidenceType END,"
                + "a.lastSmokeStatus=CASE WHEN e.passed THEN 'PASSED' ELSE 'FAILED' END,a.lastSmokeAt=e.at "
                + "RETURN count(DISTINCT l)";
    }

    private String commitUrl() {
        String root = config.httpUrl().endsWith("/")
                ? config.httpUrl().substring(0, config.httpUrl().length() - 1) : config.httpUrl();
        return root + "/db/" + config.database() + "/tx/commit";
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
