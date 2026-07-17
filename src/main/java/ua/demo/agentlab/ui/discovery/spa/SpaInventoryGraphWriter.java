package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persists raw SPA inventory separately from promoted UiStableLocator facts. */
public class SpaInventoryGraphWriter {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;

    public SpaInventoryGraphWriter(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public SpaInventoryGraphWriter(Neo4jRuntimeConfig config, JsonHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public SpaInventoryPersistenceResult persist(SpaInventoryBundle inventory) {
        if (inventory == null || inventory.pages().isEmpty()) {
            return SpaInventoryPersistenceResult.skipped("SPA inventory is empty");
        }
        if (config == null || !config.enabled()) {
            return SpaInventoryPersistenceResult.skipped("Neo4j persistence is disabled");
        }
        if (config.password() == null || config.password().isBlank()) {
            return SpaInventoryPersistenceResult.skipped("Neo4j password is not configured");
        }
        if (httpClient == null) {
            return SpaInventoryPersistenceResult.skipped("Neo4j HTTP client is unavailable");
        }
        try {
            List<Map<String, Object>> pages = new ArrayList<>();
            List<Map<String, Object>> components = new ArrayList<>();
            List<Map<String, Object>> locators = new ArrayList<>();
            List<Map<String, Object>> actions = new ArrayList<>();
            for (SpaPageInventory page : inventory.pages()) {
                pages.add(pagePayload(page));
                for (SemanticComponentInventory component : page.components()) {
                    components.add(componentPayload(page, component));
                    component.locators().forEach(locator -> locators.add(locatorPayload(page, component, locator)));
                    component.actions().forEach(action -> actions.add(actionPayload(page, component, action)));
                }
            }
            List<Map<String, Object>> statements = new ArrayList<>();
            statements.add(statement(pageMerge(), Map.of("pages", pages)));
            if (!components.isEmpty()) {
                statements.add(statement(componentMerge(), Map.of("components", components)));
            }
            if (!locators.isEmpty()) {
                statements.add(statement(locatorMerge(), Map.of("locators", locators)));
            }
            if (!actions.isEmpty()) {
                statements.add(statement(actionMerge(), Map.of("actions", actions)));
            }
            httpClient.post(commitUrl(), Map.of("statements", statements), headers());
            return new SpaInventoryPersistenceResult(true, pages.size(), components.size(), locators.size(), actions.size(),
                    "SPA candidate inventory persisted to Neo4j");
        } catch (RuntimeException exception) {
            return SpaInventoryPersistenceResult.skipped("Neo4j SPA inventory persistence failed: " + concise(exception));
        }
    }

    private Map<String, Object> statement(String cypher, Map<String, Object> parameters) {
        return Map.of("statement", cypher, "parameters", parameters);
    }

    private Map<String, Object> pagePayload(SpaPageInventory page) {
        Map<String, Object> value = metadata(page);
        value.put("pageId", page.pageId());
        value.put("pageName", page.pageName());
        value.put("route", page.route());
        value.put("capability", page.capability());
        value.put("pageFingerprintHash", page.pageFingerprintHash());
        value.put("sourceTrace", String.join("|", page.sourceTrace()));
        return value;
    }

    private Map<String, Object> componentPayload(SpaPageInventory page, SemanticComponentInventory component) {
        Map<String, Object> value = pagePayload(page);
        value.put("componentId", component.componentId());
        value.put("name", component.name());
        value.put("type", component.type().name());
        value.put("rootLocatorStrategy", component.rootLocatorStrategy());
        value.put("rootLocatorValue", component.rootLocatorValue());
        value.put("parentComponentId", component.parentComponentId());
        value.put("confidence", component.confidence());
        value.put("risks", String.join("|", component.risks()));
        value.put("sourceTrace", String.join("|", component.sourceTrace()));
        return value;
    }

    private Map<String, Object> locatorPayload(
            SpaPageInventory page,
            SemanticComponentInventory component,
            CandidateLocatorEvidence locator
    ) {
        Map<String, Object> value = componentPayload(page, component);
        value.put("locatorId", locator.locatorId());
        value.put("elementId", locator.elementId());
        value.put("strategy", locator.strategy());
        value.put("value", locator.value());
        value.put("qualityScore", locator.qualityScore());
        value.put("sameOrigin", locator.sameOrigin());
        value.put("globalMatchCount", locator.globalMatchCount());
        value.put("componentMatchCount", locator.componentMatchCount());
        value.put("uniqueOnPage", locator.uniqueOnPage());
        value.put("uniqueWithinComponent", locator.uniqueWithinComponent());
        value.put("stableAcrossRuns", locator.stableAcrossRuns());
        value.put("observedEvidenceType", locator.observedEvidenceType().name());
        value.put("risks", String.join("|", locator.risks()));
        return value;
    }

    private Map<String, Object> actionPayload(
            SpaPageInventory page,
            SemanticComponentInventory component,
            CandidateActionEvidence action
    ) {
        Map<String, Object> value = componentPayload(page, component);
        value.put("actionId", action.actionId());
        value.put("intent", action.intent());
        value.put("targetElementId", action.targetElementId());
        value.put("confidence", action.confidence());
        value.put("requiredLocatorIds", String.join("|", action.requiredLocatorIds()));
        value.put("preconditions", String.join("|", action.preconditions()));
        value.put("postconditions", String.join("|", action.postconditions()));
        value.put("sourceTrace", String.join("|", action.sourceTrace()));
        return value;
    }

    private Map<String, Object> metadata(SpaPageInventory page) {
        Map<String, Object> value = new LinkedHashMap<>();
        var metadata = page.runMetadata();
        value.put("runId", metadata == null ? "" : metadata.runId());
        value.put("appId", metadata == null ? "" : metadata.appId());
        value.put("baseUrlHash", metadata == null ? "" : metadata.baseUrlHash());
        value.put("requirementSetHash", metadata == null ? "" : metadata.requirementSetHash());
        value.put("discoverySessionId", metadata == null ? "" : metadata.discoverySessionId());
        value.put("schemaVersion", metadata == null ? SpaInventoryBundle.SCHEMA_VERSION : metadata.schemaVersion());
        value.put("createdAt", metadata == null ? "" : metadata.createdAt());
        value.put("lastSeen", metadata == null ? "" : metadata.createdAt());
        value.put("sourceAgent", metadata == null ? "ui-spa-inventory-agent" : metadata.sourceAgent());
        value.put("confidence", metadata == null ? 1.0d : metadata.confidence());
        return value;
    }

    private String pageMerge() {
        return "UNWIND $pages AS page "
                + "MERGE (p:SpaPageInventory {appId: page.appId, baseUrlHash: page.baseUrlHash, "
                + "schemaVersion: page.schemaVersion, pageId: page.pageId, pageFingerprintHash: page.pageFingerprintHash}) "
                + "SET p += page";
    }

    private String componentMerge() {
        return "UNWIND $components AS component "
                + "MATCH (p:SpaPageInventory {appId: component.appId, baseUrlHash: component.baseUrlHash, "
                + "schemaVersion: component.schemaVersion, pageId: component.pageId, pageFingerprintHash: component.pageFingerprintHash}) "
                + "MERGE (c:SpaComponentInventory {appId: component.appId, baseUrlHash: component.baseUrlHash, "
                + "schemaVersion: component.schemaVersion, pageId: component.pageId, componentId: component.componentId}) "
                + "SET c += component MERGE (p)-[:HAS_COMPONENT]->(c)";
    }

    private String locatorMerge() {
        return "UNWIND $locators AS locator "
                + "MATCH (c:SpaComponentInventory {appId: locator.appId, baseUrlHash: locator.baseUrlHash, "
                + "schemaVersion: locator.schemaVersion, pageId: locator.pageId, componentId: locator.componentId}) "
                + "MERGE (l:SpaCandidateLocator {appId: locator.appId, baseUrlHash: locator.baseUrlHash, "
                + "schemaVersion: locator.schemaVersion, pageId: locator.pageId, locatorId: locator.locatorId}) "
                + "ON CREATE SET l.status='CANDIDATE', l.verificationSuccesses=0, l.verificationFailures=0, l.smokeSuccesses=0, l.smokeFailures=0 "
                + "SET l += locator MERGE (c)-[:HAS_CANDIDATE_LOCATOR]->(l)";
    }

    private String actionMerge() {
        return "UNWIND $actions AS action "
                + "MATCH (c:SpaComponentInventory {appId: action.appId, baseUrlHash: action.baseUrlHash, "
                + "schemaVersion: action.schemaVersion, pageId: action.pageId, componentId: action.componentId}) "
                + "MERGE (a:SpaCandidateAction {appId: action.appId, baseUrlHash: action.baseUrlHash, "
                + "schemaVersion: action.schemaVersion, pageId: action.pageId, actionId: action.actionId}) "
                + "ON CREATE SET a.status='CANDIDATE', a.verificationSuccesses=0, a.verificationFailures=0, a.smokeSuccesses=0, a.smokeFailures=0 "
                + "SET a += action MERGE (c)-[:SUPPORTS_CANDIDATE_ACTION]->(a)";
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
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.replaceAll("\\s+", " ").trim();
    }
}
