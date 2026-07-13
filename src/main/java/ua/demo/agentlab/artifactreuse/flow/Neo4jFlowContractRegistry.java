package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

public class Neo4jFlowContractRegistry implements FlowContractRegistry {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;

    public Neo4jFlowContractRegistry(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public Neo4jFlowContractRegistry(Neo4jRuntimeConfig config, JsonHttpClient httpClient) {
        if (config == null || httpClient == null) {
            throw new IllegalArgumentException("flow registry dependencies cannot be null");
        }
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public FlowContractPersistenceResult persist(FlowContractBundle bundle) {
        if (!config.enabled()) {
            return FlowContractPersistenceResult.skipped("neo4j", "Neo4j flow registry is disabled");
        }
        if (config.password() == null || config.password().isBlank()) {
            return FlowContractPersistenceResult.skipped("neo4j", "Neo4j password is not configured");
        }
        if (bundle == null || bundle.contracts().isEmpty()) {
            return FlowContractPersistenceResult.skipped("neo4j", "No flow contracts to persist");
        }
        try {
            ensureConstraint();
            var response = httpClient.post(commitUrl(), Map.of("statements", List.of(Map.of(
                    "statement", mergeStatement(), "parameters", Map.of("contracts", bundle.contracts().stream()
                            .map(contract -> payload(contract, bundle.runMetadata())).toList())
            ))), headers());
            assertNoNeo4jErrors(response, "flow contract write");
            return FlowContractPersistenceResult.success("neo4j", bundle.contracts().size(),
                    "Flow contracts persisted; confirmed=" + bundle.confirmedCount());
        } catch (Exception exception) {
            return FlowContractPersistenceResult.failed("neo4j", "Flow registry write failed: " + safe(exception.getMessage()));
        }
    }

    @Override
    public FlowContractLookupResult findConfirmedByIdsWithResult(List<String> flowIds, KnowledgeRunMetadata namespace) {
        if (!config.enabled()) return FlowContractLookupResult.skipped("Neo4j flow registry is disabled");
        if (config.password() == null || config.password().isBlank()) {
            return FlowContractLookupResult.skipped("Neo4j password is not configured");
        }
        if (flowIds == null || flowIds.isEmpty()) return FlowContractLookupResult.skipped("No candidate flow IDs to verify");
        if (namespace == null || namespace.appId().isBlank() || namespace.baseUrlHash().isBlank()
                || namespace.schemaVersion().isBlank()) {
            return FlowContractLookupResult.skipped("Exact flow lookup requires complete namespace metadata");
        }
        try {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("flowIds", flowIds.stream().filter(id -> id != null && !id.isBlank()).toList());
            parameters.put("appId", namespace.appId());
            parameters.put("baseUrlHash", namespace.baseUrlHash());
            parameters.put("schemaVersion", namespace.schemaVersion());
            var response = httpClient.post(commitUrl(), Map.of("statements", List.of(Map.of(
                    "statement", findConfirmedStatement(), "parameters", parameters
            ))), headers());
            assertNoNeo4jErrors(response, "exact flow lookup");
            List<FlowContract> contracts = parseContracts(response);
            return FlowContractLookupResult.success(contracts,
                    contracts.isEmpty()
                            ? "No confirmed Neo4j flow matched the candidate IDs in the current namespace"
                            : "Confirmed " + contracts.size() + " Neo4j flow contract(s)");
        } catch (Exception exception) {
            return FlowContractLookupResult.failed("Neo4j exact flow lookup failed: " + safe(exception.getMessage()));
        }
    }

    @Override
    public FlowRuntimeFeedbackResult recordRuntimeFeedback(FlowContractBundle bundle, FlowRuntimeFeedback feedback) {
        if (!config.enabled()) return FlowRuntimeFeedbackResult.skipped("neo4j", "Neo4j flow registry is disabled");
        if (config.password() == null || config.password().isBlank()) return FlowRuntimeFeedbackResult.skipped("neo4j", "Neo4j password is not configured");
        if (bundle == null || bundle.contracts().isEmpty()) return FlowRuntimeFeedbackResult.skipped("neo4j", "No flow contracts to update");
        if (feedback == null) return FlowRuntimeFeedbackResult.skipped("neo4j", "Runtime feedback is missing");
        try {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("flowIds", bundle.contracts().stream().map(FlowContract::flowId).filter(id -> !id.isBlank()).toList());
            parameters.put("passed", feedback.smokePassed());
            parameters.put("occurredAt", feedback.occurredAt());
            parameters.put("appId", bundle.runMetadata() == null ? "" : bundle.runMetadata().appId());
            parameters.put("baseUrlHash", bundle.runMetadata() == null ? "" : bundle.runMetadata().baseUrlHash());
            var response = httpClient.post(commitUrl(), Map.of("statements", List.of(Map.of(
                    "statement", runtimeFeedbackStatement(), "parameters", parameters
            ))), headers());
            assertNoNeo4jErrors(response, "flow runtime feedback persistence");
            int updated = updatedCount(response);
            if (updated == 0) {
                return FlowRuntimeFeedbackResult.failed("neo4j",
                        "No persisted Neo4j flow contracts matched the runtime feedback namespace");
            }
            return FlowRuntimeFeedbackResult.success("neo4j", updated,
                    "Flow runtime feedback persisted; smokePassed=" + feedback.smokePassed());
        } catch (Exception exception) {
            return FlowRuntimeFeedbackResult.failed("neo4j", "Flow runtime feedback update failed: " + safe(exception.getMessage()));
        }
    }

    private Map<String, Object> payload(FlowContract contract, KnowledgeRunMetadata metadata) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("flowId", contract.flowId());
        payload.put("displayName", contract.displayName());
        payload.put("type", contract.type().name());
        payload.put("status", contract.status().name());
        payload.put("confidence", contract.confidence());
        payload.put("source", endpointPayload(contract.source()));
        payload.put("target", endpointPayload(contract.target()));
        payload.put("requiresStates", contract.requiresStates().stream().map(Enum::name).toList());
        payload.put("producesStates", contract.producesStates().stream().map(Enum::name).toList());
        payload.put("steps", contract.steps().stream().map(step -> Map.of(
                "order", step.order(),
                "action", step.action().name(),
                "ownerPage", step.ownerPage(),
                "route", step.route(),
                "dataKey", step.dataKey(),
                "setup", step.setup()
        )).toList());
        payload.put("requirementIds", contract.requirementIds());
        payload.put("assertions", contract.assertions());
        payload.put("evidence", contract.evidence());
        payload.put("artifactIds", contract.artifactIds());
        payload.put("contractFingerprint", contract.contractFingerprint());
        payload.put("lastSuccessfulSmoke", contract.lastSuccessfulSmoke());
        payload.put("runtimePassRate", contract.runtimePassRate());
        payload.put("flakyRate", contract.flakyRate());
        payload.put("runtimeSmokeAttempts", contract.runtimeSmokeAttempts());
        payload.put("runtimeSmokePasses", contract.runtimeSmokePasses());
        payload.put("lastRuntimeSmokeStatus", contract.lastRuntimeSmokeStatus());
        payload.put("appId", metadata == null ? "" : metadata.appId());
        payload.put("baseUrlHash", metadata == null ? "" : metadata.baseUrlHash());
        payload.put("schemaVersion", metadata == null ? "" : metadata.schemaVersion());
        return payload;
    }

    private Map<String, Object> endpointPayload(FlowEndpoint endpoint) {
        return Map.of(
                "pageId", endpoint.pageName().isBlank() ? endpoint.route() : endpoint.pageName(),
                "pageName", endpoint.pageName(),
                "route", endpoint.route()
        );
    }

    private String constraintStatement() {
        return "CREATE CONSTRAINT flow_contract_id IF NOT EXISTS FOR (f:FlowContract) REQUIRE f.flowId IS UNIQUE";
    }

    private String mergeStatement() {
        return """
                UNWIND $contracts AS contract
                MERGE (f:FlowContract {flowId: contract.flowId})
                WITH f, contract, coalesce(f.contractFingerprint, '') AS previousFingerprint
                SET f.displayName = contract.displayName,
                    f.type = contract.type,
                    f.status = CASE
                        WHEN previousFingerprint = contract.contractFingerprint AND f.status = 'CONFIRMED' THEN 'CONFIRMED'
                        ELSE contract.status
                    END,
                    f.confidence = contract.confidence,
                    f.contractFingerprint = contract.contractFingerprint,
                    f.lastSuccessfulSmoke = CASE
                        WHEN previousFingerprint = contract.contractFingerprint THEN coalesce(f.lastSuccessfulSmoke, contract.lastSuccessfulSmoke)
                        ELSE contract.lastSuccessfulSmoke
                    END,
                    f.runtimePassRate = CASE
                        WHEN previousFingerprint = contract.contractFingerprint THEN coalesce(f.runtimePassRate, contract.runtimePassRate)
                        ELSE contract.runtimePassRate
                    END,
                    f.flakyRate = CASE
                        WHEN previousFingerprint = contract.contractFingerprint THEN coalesce(f.flakyRate, contract.flakyRate)
                        ELSE contract.flakyRate
                    END,
                    f.runtimeSmokeAttempts = CASE
                        WHEN previousFingerprint = contract.contractFingerprint THEN coalesce(f.runtimeSmokeAttempts, 0)
                        ELSE contract.runtimeSmokeAttempts
                    END,
                    f.runtimeSmokePasses = CASE
                        WHEN previousFingerprint = contract.contractFingerprint THEN coalesce(f.runtimeSmokePasses, 0)
                        ELSE contract.runtimeSmokePasses
                    END,
                    f.lastRuntimeSmokeStatus = CASE
                        WHEN previousFingerprint = contract.contractFingerprint THEN coalesce(f.lastRuntimeSmokeStatus, contract.lastRuntimeSmokeStatus)
                        ELSE contract.lastRuntimeSmokeStatus
                    END,
                    f.appId = contract.appId,
                    f.baseUrlHash = contract.baseUrlHash,
                    f.schemaVersion = contract.schemaVersion,
                    f.requirementIds = contract.requirementIds,
                    f.assertions = contract.assertions,
                    f.evidence = contract.evidence
                WITH f, contract
                MERGE (source:Page {pageId: contract.source.pageId})
                SET source.pageName = contract.source.pageName,
                    source.route = contract.source.route
                MERGE (target:Page {pageId: contract.target.pageId})
                SET target.pageName = contract.target.pageName,
                    target.route = contract.target.route
                MERGE (f)-[:STARTS_AT]->(source)
                MERGE (f)-[:ENDS_AT]->(target)
                WITH f, contract
                UNWIND contract.requiresStates AS requiredState
                MERGE (required:FlowState {stateId: requiredState})
                MERGE (f)-[:REQUIRES_STATE]->(required)
                WITH f, contract
                UNWIND contract.producesStates AS producedState
                MERGE (produced:FlowState {stateId: producedState})
                MERGE (f)-[:PRODUCES_STATE]->(produced)
                WITH f, contract
                UNWIND contract.steps AS step
                MERGE (flowStep:FlowStep {flowId: contract.flowId, stepOrder: step.order})
                SET flowStep.action = step.action,
                    flowStep.ownerPage = step.ownerPage,
                    flowStep.route = step.route,
                    flowStep.dataKey = step.dataKey,
                    flowStep.setup = step.setup
                MERGE (f)-[:HAS_STEP]->(flowStep)
                WITH f, contract
                UNWIND contract.artifactIds AS artifactId
                MATCH (artifact:Artifact {artifactId: artifactId})
                MERGE (f)-[:USES_ARTIFACT]->(artifact)
                RETURN count(f) AS flowCount
                """;
    }

    private String findConfirmedStatement() {
        return """
                MATCH (f:FlowContract)
                WHERE f.flowId IN $flowIds
                  AND f.status = 'CONFIRMED'
                  AND ($appId = '' OR f.appId = $appId)
                  AND ($baseUrlHash = '' OR f.baseUrlHash = $baseUrlHash)
                  AND ($schemaVersion = '' OR f.schemaVersion = $schemaVersion)
                OPTIONAL MATCH (f)-[:STARTS_AT]->(source:Page)
                OPTIONAL MATCH (f)-[:ENDS_AT]->(target:Page)
                RETURN f.flowId AS flowId, f.displayName AS displayName, f.type AS type,
                       f.confidence AS confidence, f.assertions AS assertions,
                       f.requirementIds AS requirementIds, f.evidence AS evidence,
                       source.pageName AS sourcePageName, source.route AS sourceRoute,
                       target.pageName AS targetPageName, target.route AS targetRoute,
                       f.contractFingerprint AS contractFingerprint,
                       f.lastSuccessfulSmoke AS lastSuccessfulSmoke,
                       f.runtimePassRate AS runtimePassRate, f.flakyRate AS flakyRate,
                       f.runtimeSmokeAttempts AS runtimeSmokeAttempts,
                       f.runtimeSmokePasses AS runtimeSmokePasses,
                       f.lastRuntimeSmokeStatus AS lastRuntimeSmokeStatus
                """;
    }

    private String runtimeFeedbackStatement() {
        return """
                MATCH (f:FlowContract)
                WHERE f.flowId IN $flowIds
                  AND ($appId = '' OR f.appId = $appId)
                  AND ($baseUrlHash = '' OR f.baseUrlHash = $baseUrlHash)
                WITH f, $passed AS passed, $occurredAt AS occurredAt,
                     coalesce(f.runtimeSmokeAttempts, 0) + 1 AS attempts,
                     coalesce(f.runtimeSmokePasses, 0) + CASE WHEN $passed THEN 1 ELSE 0 END AS passes
                SET f.runtimeSmokeAttempts = attempts,
                    f.runtimeSmokePasses = passes,
                    f.runtimePassRate = toFloat(passes) / attempts,
                    f.flakyRate = 1.0 - (toFloat(passes) / attempts),
                    f.lastSuccessfulSmoke = CASE WHEN passed THEN occurredAt ELSE coalesce(f.lastSuccessfulSmoke, '') END,
                    f.lastRuntimeSmokeStatus = CASE WHEN passed THEN 'PASSED' ELSE 'FAILED' END,
                    f.lastRuntimeSmokeAt = occurredAt
                RETURN count(f) AS updated
                """;
    }

    private List<FlowContract> parseContracts(com.fasterxml.jackson.databind.JsonNode response) {
        if (response == null) {
            return List.of();
        }
        var data = response.path("results").path(0).path("data");
        if (!data.isArray()) {
            return List.of();
        }
        List<FlowContract> contracts = new java.util.ArrayList<>();
        data.forEach(row -> {
            var values = row.path("row");
            if (!values.isArray() || values.size() < 18) {
                return;
            }
            try {
                FlowContractType type = FlowContractType.valueOf(values.path(2).asText("GENERIC"));
                contracts.add(new FlowContract("flow-contract.v1", values.path(0).asText(), values.path(1).asText(), type,
                        new FlowEndpoint(values.path(7).asText(), values.path(8).asText()),
                        new FlowEndpoint(values.path(9).asText(), values.path(10).asText()),
                        List.of(), List.of(), List.of(), textValues(values.path(4)), textValues(values.path(5)),
                        textValues(values.path(6)), List.of(), values.path(11).asText(), values.path(12).asText(),
                        values.path(13).asDouble(0.0d), values.path(14).asDouble(1.0d),
                        values.path(15).asInt(0), values.path(16).asInt(0), values.path(17).asText(""),
                        values.path(3).asDouble(0.0d), FlowContractStatus.CONFIRMED));
            } catch (Exception ignored) {
                // One malformed historical record must not poison semantic candidate validation.
            }
        });
        return List.copyOf(contracts);
    }

    private List<String> textValues(com.fasterxml.jackson.databind.JsonNode values) {
        if (values == null || !values.isArray()) {
            return List.of();
        }
        List<String> result = new java.util.ArrayList<>();
        values.forEach(value -> {
            if (value != null && value.isTextual() && !value.asText().isBlank()) result.add(value.asText());
        });
        return List.copyOf(result);
    }

    private void assertNoNeo4jErrors(com.fasterxml.jackson.databind.JsonNode response, String operation) {
        if (response == null) {
            throw new IllegalStateException("Neo4j " + operation + " returned no response");
        }
        var errors = response.path("errors");
        if (!errors.isArray() || errors.isEmpty()) {
            return;
        }
        var first = errors.get(0);
        String code = first.path("code").asText("");
        String message = first.path("message").asText("Neo4j returned an unknown error");
        throw new IllegalStateException((code.isBlank() ? "" : code + ": ") + message);
    }

    private void ensureConstraint() {
        var response = httpClient.post(commitUrl(), Map.of("statements", List.of(Map.of(
                "statement", constraintStatement(), "parameters", Map.of()
        ))), headers());
        assertNoNeo4jErrors(response, "flow contract schema setup");
    }

    private int updatedCount(com.fasterxml.jackson.databind.JsonNode response) {
        if (response == null) return 0;
        var rows = response.path("results").path(0).path("data");
        if (!rows.isArray() || rows.isEmpty()) return 0;
        return Math.max(0, rows.path(0).path("row").path(0).asInt(0));
    }

    private String commitUrl() {
        String base = config.httpUrl().endsWith("/")
                ? config.httpUrl().substring(0, config.httpUrl().length() - 1)
                : config.httpUrl();
        return base + "/db/" + config.database() + "/tx/commit";
    }

    private Map<String, String> headers() {
        return Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.password()).getBytes(StandardCharsets.UTF_8)
        ));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
