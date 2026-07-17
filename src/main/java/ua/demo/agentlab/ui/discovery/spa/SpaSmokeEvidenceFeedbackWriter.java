package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaSmokeEvidenceFeedbackResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ai.ui.contract.PomSourceMap;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Links an actually generated POM member to SPA candidate evidence after live browser smoke. */
public class SpaSmokeEvidenceFeedbackWriter {
    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient http;

    public SpaSmokeEvidenceFeedbackWriter(Neo4jRuntimeConfig config) { this(config, new JsonHttpClient()); }
    public SpaSmokeEvidenceFeedbackWriter(Neo4jRuntimeConfig config, JsonHttpClient http) { this.config = config; this.http = http; }

    public SpaSmokeEvidenceFeedbackResult write(boolean liveSmokePassed, PomSourceMap sourceMap,
                                                 SpaInventoryBundle inventory, SpaTargetedVerificationResult verification,
                                                 SpaInventoryConfig policy) {
        if (inventory == null || verification == null || sourceMap == null || sourceMap.entries().isEmpty()) return SpaSmokeEvidenceFeedbackResult.skipped("POM source map or SPA verification evidence is missing");
        if (config == null || !config.enabled()) return SpaSmokeEvidenceFeedbackResult.skipped("Neo4j persistence is disabled");
        if (config.password() == null || config.password().isBlank()) return SpaSmokeEvidenceFeedbackResult.skipped("Neo4j password is not configured");
        List<TargetedLocatorVerification> linkedLocators = verification.locatorVerifications().stream()
                .filter(TargetedLocatorVerification::verified).filter(locator -> appearsInSourceMap(locator, sourceMap)).toList();
        Map<String, TargetedLocatorVerification> locatorIndex = new LinkedHashMap<>();
        linkedLocators.forEach(locator -> locatorIndex.put(locator.locatorId(), locator));
        List<TargetedActionVerification> linkedActions = verification.actionVerifications().stream()
                .filter(TargetedActionVerification::verified)
                .filter(action -> actionLocatorsPresent(action, inventory, locatorIndex)).toList();
        if (linkedLocators.isEmpty() && linkedActions.isEmpty()) return SpaSmokeEvidenceFeedbackResult.skipped("No targeted SPA evidence is referenced by generated POM source");
        try {
            var metadata = verification.runMetadata();
            String now = Instant.now().toString();
            List<Map<String,Object>> locators = linkedLocators.stream().map(locator -> Map.<String,Object>of(
                    "appId", metadata.appId(), "baseUrlHash", metadata.baseUrlHash(), "schemaVersion", metadata.schemaVersion(),
                    "pageId", locator.pageId(), "locatorId", locator.locatorId(), "passed", liveSmokePassed, "at", now)).toList();
            List<Map<String,Object>> actions = linkedActions.stream().map(action -> Map.<String,Object>of(
                    "appId", metadata.appId(), "baseUrlHash", metadata.baseUrlHash(), "schemaVersion", metadata.schemaVersion(),
                    "pageId", action.pageId(), "actionId", action.actionId(), "passed", liveSmokePassed, "at", now)).toList();
            http.post(commitUrl(), Map.of("statements", List.of(
                    Map.of("statement", locatorStatement(), "parameters", Map.of("evidence", locators, "promoteAfter", policy.promoteAfterSuccesses(), "demoteAfter", policy.demoteAfterFailures())),
                    Map.of("statement", actionStatement(), "parameters", Map.of("evidence", actions, "promoteAfter", policy.promoteAfterSuccesses(), "demoteAfter", policy.demoteAfterFailures())))), headers());
            return new SpaSmokeEvidenceFeedbackResult(true, liveSmokePassed, linkedLocators.size(), linkedActions.size(),
                    "Generated POM smoke feedback linked to SPA candidate evidence", List.of("live-smoke=" + liveSmokePassed));
        } catch (RuntimeException exception) {
            return SpaSmokeEvidenceFeedbackResult.skipped("Neo4j SPA smoke feedback failed: " + concise(exception));
        }
    }

    private boolean appearsInSourceMap(TargetedLocatorVerification locator, PomSourceMap sourceMap) {
        return sourceMap.entries().stream().anyMatch(page -> page.fields().stream().anyMatch(field ->
                field.strategy().equalsIgnoreCase(locator.strategy()) && field.value().equals(locator.value())));
    }

    private boolean actionLocatorsPresent(TargetedActionVerification action, SpaInventoryBundle inventory,
                                          Map<String, TargetedLocatorVerification> locators) {
        CandidateActionEvidence candidate = inventory.pages().stream().filter(page -> page.pageId().equals(action.pageId()))
                .flatMap(page -> page.components().stream()).flatMap(component -> component.actions().stream())
                .filter(item -> item.actionId().equals(action.actionId())).findFirst().orElse(null);
        return candidate != null && !candidate.requiredLocatorIds().isEmpty()
                && candidate.requiredLocatorIds().stream().allMatch(locators::containsKey);
    }

    private String locatorStatement() {
        return "UNWIND $evidence AS e MATCH (l:SpaCandidateLocator {appId:e.appId, baseUrlHash:e.baseUrlHash, schemaVersion:e.schemaVersion, pageId:e.pageId, locatorId:e.locatorId}) "
                + "WITH l,e, CASE WHEN e.passed THEN coalesce(l.smokeSuccesses,0)+1 ELSE coalesce(l.smokeSuccesses,0) END AS successes, "
                + "CASE WHEN e.passed THEN coalesce(l.smokeFailures,0) ELSE coalesce(l.smokeFailures,0)+1 END AS failures "
                + "SET l.smokeSuccesses=successes,l.smokeFailures=failures,l.lastSmokeStatus=CASE WHEN e.passed THEN 'PASSED' ELSE 'FAILED' END,l.lastSmokeAt=e.at, "
                + "l.lastSuccessfulSmoke=CASE WHEN e.passed THEN e.at ELSE coalesce(l.lastSuccessfulSmoke,'') END, "
                + "l.status=CASE WHEN failures >= $demoteAfter THEN 'DEGRADED' "
                + "WHEN e.passed AND l.status IN ['STABLE','PROMPT_ALLOWED'] AND successes >= 1 "
                + "AND coalesce(l.qualityScore,0) >= 0.80 THEN 'PROMPT_ALLOWED' ELSE coalesce(l.status,'CANDIDATE') END";
    }
    private String actionStatement() {
        return "UNWIND $evidence AS e MATCH (a:SpaCandidateAction {appId:e.appId, baseUrlHash:e.baseUrlHash, schemaVersion:e.schemaVersion, pageId:e.pageId, actionId:e.actionId}) "
                + "WITH a,e, CASE WHEN e.passed THEN coalesce(a.smokeSuccesses,0)+1 ELSE coalesce(a.smokeSuccesses,0) END AS successes, "
                + "CASE WHEN e.passed THEN coalesce(a.smokeFailures,0) ELSE coalesce(a.smokeFailures,0)+1 END AS failures "
                + "SET a.smokeSuccesses=successes,a.smokeFailures=failures,a.lastSmokeStatus=CASE WHEN e.passed THEN 'PASSED' ELSE 'FAILED' END,a.lastSmokeAt=e.at, "
                + "a.lastSuccessfulSmoke=CASE WHEN e.passed THEN e.at ELSE coalesce(a.lastSuccessfulSmoke,'') END, "
                + "a.status=CASE WHEN failures >= $demoteAfter THEN 'DEGRADED' "
                + "WHEN e.passed AND a.status IN ['STABLE','PROMPT_ALLOWED'] AND successes >= 1 "
                + "AND coalesce(a.confidence,0) >= 0.80 THEN 'PROMPT_ALLOWED' ELSE coalesce(a.status,'CANDIDATE') END";
    }
    private String commitUrl() { String root=config.httpUrl().endsWith("/")?config.httpUrl().substring(0,config.httpUrl().length()-1):config.httpUrl(); return root+"/db/"+config.database()+"/tx/commit"; }
    private Map<String,String> headers() { return Map.of("Authorization", "Basic "+Base64.getEncoder().encodeToString((config.username()+":"+config.password()).getBytes(StandardCharsets.UTF_8))); }
    private String concise(RuntimeException e) { return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage().replaceAll("\\s+"," ").trim(); }
}
