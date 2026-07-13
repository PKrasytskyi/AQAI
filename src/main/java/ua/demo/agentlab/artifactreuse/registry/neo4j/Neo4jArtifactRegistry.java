package ua.demo.agentlab.artifactreuse.registry.neo4j;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.artifactreuse.model.ArtifactRecord;
import ua.demo.agentlab.artifactreuse.model.ArtifactRunRelation;
import ua.demo.agentlab.artifactreuse.model.ArtifactStatus;
import ua.demo.agentlab.artifactreuse.model.ArtifactTarget;
import ua.demo.agentlab.artifactreuse.model.ArtifactTargetType;
import ua.demo.agentlab.artifactreuse.model.ArtifactType;
import ua.demo.agentlab.artifactreuse.model.QualityGateRecord;
import ua.demo.agentlab.artifactreuse.model.RunRecord;
import ua.demo.agentlab.artifactreuse.registry.ArtifactLookupRequest;
import ua.demo.agentlab.artifactreuse.registry.ArtifactLookupResult;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistry;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteRequest;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteResult;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Neo4jArtifactRegistry implements ArtifactRegistry {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;
    private final Neo4jArtifactRegistrySchema schema;

    public Neo4jArtifactRegistry(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient(), new Neo4jArtifactRegistrySchema());
    }

    public Neo4jArtifactRegistry(
            Neo4jRuntimeConfig config,
            JsonHttpClient httpClient,
            Neo4jArtifactRegistrySchema schema
    ) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (httpClient == null || schema == null) {
            throw new IllegalArgumentException("registry dependencies cannot be null");
        }
        this.config = config;
        this.httpClient = httpClient;
        this.schema = schema;
    }

    @Override
    public ArtifactRegistryWriteResult register(ArtifactRegistryWriteRequest request) {
        if (!config.enabled()) {
            return ArtifactRegistryWriteResult.skipped("neo4j", "Neo4j artifact registry is disabled");
        }
        if (isBlank(config.password())) {
            return ArtifactRegistryWriteResult.skipped("neo4j", "Neo4j password is not configured");
        }
        validate(request);
        try {
            ensureSchema();
            JsonNode response = httpClient.post(commitUrl(), Map.of("statements", List.of(Map.of(
                    "statement", mergeArtifactGraphStatement(request.runRelation()),
                    "parameters", parameters(request)
            ))), headers());
            assertNoNeo4jErrors(response, "artifact registry write");
            return ArtifactRegistryWriteResult.success(
                    "neo4j",
                    1,
                    "Artifact registry metadata written for artifactId=" + request.artifact().artifactId()
            );
        } catch (Exception exception) {
            return ArtifactRegistryWriteResult.failed(
                    "neo4j",
                    1,
                    "Neo4j artifact registry write failed: " + safe(exception.getMessage())
            );
        }
    }

    @Override
    public ArtifactLookupResult findStableArtifact(ArtifactLookupRequest request) {
        if (!config.enabled()) {
            return ArtifactLookupResult.skipped("neo4j", "Neo4j artifact registry is disabled");
        }
        if (isBlank(config.password())) {
            return ArtifactLookupResult.skipped("neo4j", "Neo4j password is not configured");
        }
        if (request == null || !request.complete()) {
            return ArtifactLookupResult.miss("neo4j", "artifact lookup request is incomplete");
        }
        try {
            JsonNode response = httpClient.post(commitUrl(), Map.of("statements", List.of(Map.of(
                    "statement", findStableArtifactStatement(),
                    "parameters", Map.of(
                            "artifactType", request.artifactType().name(),
                            "targetId", request.targetId(),
                            "fingerprint", request.fingerprint(),
                            "schemaVersion", request.schemaVersion()
                    )
            ))), headers());
            assertNoNeo4jErrors(response, "artifact registry lookup");
            ArtifactRecord artifact = readArtifact(response);
            if (artifact == null) {
                return ArtifactLookupResult.miss("neo4j", "stable artifact metadata not found");
            }
            return ArtifactLookupResult.hit("neo4j", artifact);
        } catch (Exception exception) {
            return ArtifactLookupResult.miss("neo4j",
                    "Neo4j artifact lookup failed: " + safe(exception.getMessage()));
        }
    }

    private void validate(ArtifactRegistryWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (request.artifact() == null || !request.artifact().hasIdentity()) {
            throw new IllegalArgumentException("artifact must include artifactId, targetId, and fingerprint");
        }
        if (request.target() == null || request.target().targetId().isBlank()) {
            throw new IllegalArgumentException("target must include targetId");
        }
        if (request.run() == null || request.run().runId().isBlank()) {
            throw new IllegalArgumentException("run must include runId");
        }
    }

    private Map<String, Object> parameters(ArtifactRegistryWriteRequest request) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("artifact", artifactPayload(request.artifact()));
        parameters.put("target", targetPayload(request.target()));
        parameters.put("run", runPayload(request.run()));
        parameters.put("qualityGates", request.qualityGates().stream().map(this::qualityGatePayload).toList());
        return parameters;
    }

    private Map<String, Object> artifactPayload(ArtifactRecord artifact) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("artifactId", artifact.artifactId());
        payload.put("artifactType", artifact.artifactType().name());
        payload.put("targetType", artifact.targetType().name());
        payload.put("targetId", artifact.targetId());
        payload.put("fingerprint", artifact.fingerprint());
        payload.put("schemaVersion", artifact.schemaVersion());
        payload.put("promptTemplateVersion", artifact.promptTemplateVersion());
        payload.put("model", artifact.model());
        payload.put("temperature", artifact.temperature());
        payload.put("status", artifact.status().name());
        payload.put("qualityScore", artifact.qualityScore());
        payload.put("writerSucceeded", artifact.writerSucceeded());
        payload.put("compileSucceeded", artifact.compileSucceeded());
        payload.put("filePath", artifact.filePath());
        payload.put("createdAt", artifact.createdAt());
        payload.put("lastUsedAt", artifact.lastUsedAt());
        payload.put("reuseCount", artifact.reuseCount());
        return payload;
    }

    private Map<String, Object> targetPayload(ArtifactTarget target) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetType", target.targetType().name());
        payload.put("targetId", target.targetId());
        payload.put("targetKey", target.targetType().name() + ":" + target.targetId());
        payload.put("pageId", target.pageId().isBlank() ? target.targetId() : target.pageId());
        payload.put("pageName", target.pageName());
        payload.put("route", target.route());
        payload.put("capability", target.capability());
        return payload;
    }

    private Map<String, Object> runPayload(RunRecord run) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", run.runId());
        payload.put("appId", run.appId());
        payload.put("baseUrlHash", run.baseUrlHash());
        payload.put("requirementSetHash", run.requirementSetHash());
        payload.put("discoverySessionId", run.discoverySessionId());
        payload.put("schemaVersion", run.schemaVersion());
        payload.put("createdAt", run.createdAt());
        payload.put("sourceAgent", run.sourceAgent());
        return payload;
    }

    private Map<String, Object> qualityGatePayload(QualityGateRecord gate) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gateId", gate.gateId());
        payload.put("gateType", gate.gateType());
        payload.put("status", gate.status());
        payload.put("summary", gate.summary());
        payload.put("issueCount", gate.issueCount());
        payload.put("createdAt", gate.createdAt());
        return payload;
    }

    private String mergeArtifactGraphStatement(ArtifactRunRelation relation) {
        String runRelation = relation == ArtifactRunRelation.REUSED ? "REUSED" : "PRODUCED";
        return """
                MERGE (a:Artifact {artifactId: $artifact.artifactId})
                SET a.artifactType = $artifact.artifactType,
                    a.targetType = $artifact.targetType,
                    a.targetId = $artifact.targetId,
                    a.fingerprint = $artifact.fingerprint,
                    a.schemaVersion = $artifact.schemaVersion,
                    a.promptTemplateVersion = $artifact.promptTemplateVersion,
                    a.model = $artifact.model,
                    a.temperature = $artifact.temperature,
                    a.status = $artifact.status,
                    a.qualityScore = $artifact.qualityScore,
                    a.writerSucceeded = $artifact.writerSucceeded,
                    a.compileSucceeded = $artifact.compileSucceeded,
                    a.filePath = $artifact.filePath,
                    a.createdAt = CASE WHEN a.createdAt IS NULL OR a.createdAt = '' THEN $artifact.createdAt ELSE a.createdAt END,
                    a.lastUsedAt = $artifact.lastUsedAt,
                    a.reuseCount = $artifact.reuseCount
                // Keep registry targets separate from knowledge-graph Page nodes.
                MERGE (p:ArtifactTarget {targetKey: $target.targetKey})
                SET p.pageId = $target.pageId,
                    p.pageName = $target.pageName,
                    p.route = $target.route,
                    p.capability = $target.capability,
                    p.targetType = $target.targetType,
                    p.targetId = $target.targetId
                MERGE (r:ArtifactRegistryRun {runId: $run.runId})
                SET r.appId = $run.appId,
                    r.baseUrlHash = $run.baseUrlHash,
                    r.requirementSetHash = $run.requirementSetHash,
                    r.discoverySessionId = $run.discoverySessionId,
                    r.schemaVersion = $run.schemaVersion,
                    r.createdAt = CASE WHEN r.createdAt IS NULL OR r.createdAt = '' THEN $run.createdAt ELSE r.createdAt END,
                    r.sourceAgent = $run.sourceAgent
                MERGE (a)-[:GENERATED_FOR]->(p)
                MERGE (r)-[runRel:%s]->(a)
                SET runRel.updatedAt = coalesce($artifact.lastUsedAt, $artifact.createdAt)
                WITH a
                UNWIND $qualityGates AS gate
                MERGE (q:ArtifactValidationGate {gateId: gate.gateId})
                SET q.gateType = gate.gateType,
                    q.status = gate.status,
                    q.summary = gate.summary,
                    q.issueCount = gate.issueCount,
                    q.createdAt = gate.createdAt
                MERGE (a)-[validation:VALIDATED_BY]->(q)
                SET validation.status = gate.status,
                    validation.updatedAt = gate.createdAt
                RETURN a.artifactId
                """.formatted(runRelation);
    }

    private String findStableArtifactStatement() {
        return """
                MATCH (a:Artifact {artifactType: $artifactType, targetId: $targetId, fingerprint: $fingerprint})
                WHERE ($schemaVersion = '' OR a.schemaVersion = $schemaVersion)
                  AND a.status = 'STABLE'
                RETURN a {
                    .artifactId,
                    .artifactType,
                    .targetType,
                    .targetId,
                    .fingerprint,
                    .schemaVersion,
                    .promptTemplateVersion,
                    .model,
                    .temperature,
                    .status,
                    .qualityScore,
                    .writerSucceeded,
                    .compileSucceeded,
                    .filePath,
                    .createdAt,
                    .lastUsedAt,
                    .reuseCount
                } AS artifact
                ORDER BY coalesce(a.qualityScore, 0) DESC, coalesce(a.lastUsedAt, a.createdAt) DESC
                LIMIT 1
                """;
    }

    private ArtifactRecord readArtifact(JsonNode response) {
        JsonNode row = response.path("results").path(0).path("data").path(0).path("row").path(0);
        if (row.isMissingNode() || row.isNull()) {
            return null;
        }
        return new ArtifactRecord(
                text(row, "artifactId"),
                artifactType(text(row, "artifactType")),
                targetType(text(row, "targetType")),
                text(row, "targetId"),
                text(row, "fingerprint"),
                text(row, "schemaVersion"),
                text(row, "promptTemplateVersion"),
                text(row, "model"),
                row.path("temperature").asDouble(0.0d),
                artifactStatus(text(row, "status")),
                row.path("qualityScore").asDouble(0.0d),
                row.path("writerSucceeded").asBoolean(false),
                row.path("compileSucceeded").asBoolean(false),
                text(row, "filePath"),
                text(row, "createdAt"),
                text(row, "lastUsedAt"),
                row.path("reuseCount").asInt(0)
        );
    }

    private void ensureSchema() {
        JsonNode response = httpClient.post(commitUrl(), Map.of("statements", schema.constraintStatements()), headers());
        assertNoNeo4jErrors(response, "artifact registry schema setup");
    }

    private void assertNoNeo4jErrors(JsonNode response, String operation) {
        if (response == null) {
            throw new IllegalStateException("Neo4j " + operation + " returned no response");
        }
        JsonNode errors = response.path("errors");
        if (!errors.isArray() || errors.isEmpty()) {
            return;
        }
        JsonNode first = errors.get(0);
        String code = first.path("code").asText("");
        String message = first.path("message").asText("Neo4j returned an unknown error");
        throw new IllegalStateException((code.isBlank() ? "" : code + ": ") + message);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private ArtifactType artifactType(String value) {
        try {
            return ArtifactType.valueOf(safe(value).isBlank() ? "UNKNOWN" : safe(value).toUpperCase());
        } catch (Exception exception) {
            return ArtifactType.UNKNOWN;
        }
    }

    private ArtifactTargetType targetType(String value) {
        try {
            return ArtifactTargetType.valueOf(safe(value).isBlank() ? "UNKNOWN" : safe(value).toUpperCase());
        } catch (Exception exception) {
            return ArtifactTargetType.UNKNOWN;
        }
    }

    private ArtifactStatus artifactStatus(String value) {
        try {
            return ArtifactStatus.valueOf(safe(value).isBlank() ? "GENERATED" : safe(value).toUpperCase());
        } catch (Exception exception) {
            return ArtifactStatus.GENERATED;
        }
    }

    private String commitUrl() {
        return trimTrailingSlash(config.httpUrl()) + "/db/" + config.database() + "/tx/commit";
    }

    private Map<String, String> headers() {
        return Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.password()).getBytes(StandardCharsets.UTF_8)
        ));
    }

    private String trimTrailingSlash(String value) {
        String safe = safe(value);
        return safe.endsWith("/") ? safe.substring(0, safe.length() - 1) : safe;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
