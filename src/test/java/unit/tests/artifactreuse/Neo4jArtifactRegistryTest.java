package unit.tests.artifactreuse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.artifactreuse.model.ArtifactRecord;
import ua.demo.agentlab.artifactreuse.model.ArtifactRunRelation;
import ua.demo.agentlab.artifactreuse.model.ArtifactStatus;
import ua.demo.agentlab.artifactreuse.model.ArtifactTarget;
import ua.demo.agentlab.artifactreuse.model.ArtifactTargetType;
import ua.demo.agentlab.artifactreuse.model.ArtifactType;
import ua.demo.agentlab.artifactreuse.model.QualityGateRecord;
import ua.demo.agentlab.artifactreuse.model.RunRecord;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteRequest;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteResult;
import ua.demo.agentlab.artifactreuse.registry.neo4j.Neo4jArtifactRegistry;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class Neo4jArtifactRegistryTest {

    @Test
    public void writesArtifactRegistrySchemaAndProducedRelationships() {
        CapturingHttpClient http = new CapturingHttpClient();
        Neo4jArtifactRegistry registry = new Neo4jArtifactRegistry(new EnabledConfig(), http,
                new ua.demo.agentlab.artifactreuse.registry.neo4j.Neo4jArtifactRegistrySchema());

        ArtifactRegistryWriteResult result = registry.register(request(ArtifactRunRelation.PRODUCED));

        Assert.assertTrue(result.success());
        Assert.assertEquals(http.url, "http://localhost:7474/db/neo4j/tx/commit");
        Assert.assertTrue(http.headers.containsKey("Authorization"));
        Assert.assertEquals(http.bodies.size(), 2, "Schema and artifact graph writes must use separate transactions");
        List<Map<String, Object>> schemaStatements = statements(http.bodies.get(0));
        Assert.assertTrue(schemaStatements.stream().anyMatch(statement ->
                statement.get("statement").toString().contains("CREATE CONSTRAINT artifact_target_key")));
        List<Map<String, Object>> statements = statements(http.bodies.get(1));
        Assert.assertTrue(statements.stream().anyMatch(statement ->
                statement.get("statement").toString().contains("MERGE (r)-[runRel:PRODUCED]->(a)")));
        Assert.assertTrue(statements.stream().anyMatch(statement ->
                statement.get("statement").toString().contains("MERGE (p:ArtifactTarget {targetKey: $target.targetKey})")));
        Assert.assertTrue(statements.stream().anyMatch(statement ->
                statement.get("statement").toString().contains("MERGE (r:ArtifactRegistryRun {runId: $run.runId})")));
        Map<String, Object> parameters = parameters(statements.get(0));
        Map<String, Object> artifact = map(parameters.get("artifact"));
        Assert.assertEquals(artifact.get("artifactId"), "artifact-login-pom");
        Assert.assertEquals(artifact.get("artifactType"), "POM_CONTRACT");
        Assert.assertEquals(artifact.get("status"), "STABLE");
        Map<String, Object> target = map(parameters.get("target"));
        Assert.assertEquals(target.get("pageId"), "page-login");
        Assert.assertEquals(target.get("targetKey"), "PAGE:page-login");
        Map<String, Object> run = map(parameters.get("run"));
        Assert.assertEquals(run.get("runId"), "run-1");
        Assert.assertEquals(((List<?>) parameters.get("qualityGates")).size(), 2);
    }

    @Test
    public void writesReusedRunRelationshipWhenArtifactIsReused() {
        CapturingHttpClient http = new CapturingHttpClient();
        Neo4jArtifactRegistry registry = new Neo4jArtifactRegistry(new EnabledConfig(), http,
                new ua.demo.agentlab.artifactreuse.registry.neo4j.Neo4jArtifactRegistrySchema());

        ArtifactRegistryWriteResult result = registry.register(request(ArtifactRunRelation.REUSED));

        Assert.assertTrue(result.success());
        Assert.assertTrue(statements(http.bodies.get(1)).stream().anyMatch(statement ->
                statement.get("statement").toString().contains("MERGE (r)-[runRel:REUSED]->(a)")));
    }

    @Test
    public void skipsWhenNeo4jIsDisabled() {
        CapturingHttpClient http = new CapturingHttpClient();
        Neo4jArtifactRegistry registry = new Neo4jArtifactRegistry(new DisabledConfig(), http,
                new ua.demo.agentlab.artifactreuse.registry.neo4j.Neo4jArtifactRegistrySchema());

        ArtifactRegistryWriteResult result = registry.register(request(ArtifactRunRelation.PRODUCED));

        Assert.assertFalse(result.attempted());
        Assert.assertFalse(result.success());
        Assert.assertTrue(http.bodies.isEmpty());
    }

    @Test
    public void reportsNeo4jTransactionErrorsInsteadOfClaimingArtifactWasRegistered() {
        CapturingHttpClient http = new CapturingHttpClient();
        http.response = responseWithError("Neo.ClientError.Statement.SyntaxError", "bad cypher");
        Neo4jArtifactRegistry registry = new Neo4jArtifactRegistry(new EnabledConfig(), http,
                new ua.demo.agentlab.artifactreuse.registry.neo4j.Neo4jArtifactRegistrySchema());

        ArtifactRegistryWriteResult result = registry.register(request(ArtifactRunRelation.PRODUCED));

        Assert.assertFalse(result.success());
        Assert.assertTrue(result.message().contains("Neo.ClientError.Statement.SyntaxError"));
    }

    private ArtifactRegistryWriteRequest request(ArtifactRunRelation relation) {
        return new ArtifactRegistryWriteRequest(
                new ArtifactRecord(
                        "artifact-login-pom",
                        ArtifactType.POM_CONTRACT,
                        ArtifactTargetType.PAGE,
                        "page-login",
                        "fingerprint-123",
                        "pom-contract-v1",
                        "pom-json-generation-v1",
                        "gpt-5-mini",
                        0.0d,
                        ArtifactStatus.STABLE,
                        94.0d,
                        true,
                        true,
                        "target/ai-run-history/stable/pom-contracts/LoginPage.fingerprint-123.json",
                        "2026-07-11T00:00:00Z",
                        "2026-07-11T00:01:00Z",
                        relation == ArtifactRunRelation.REUSED ? 2 : 0
                ),
                ArtifactTarget.page("page-login", "LoginPage", "/login", "AUTHENTICATION"),
                new RunRecord(
                        "run-1",
                        "app",
                        "base",
                        "requirements",
                        "discovery",
                        "knowledge-v1",
                        "2026-07-11T00:00:00Z",
                        "test"
                ),
                relation,
                List.of(
                        new QualityGateRecord("gate-schema", "SCHEMA", "PASSED", "schema ok", 0, "2026-07-11T00:00:01Z"),
                        new QualityGateRecord("gate-compile", "COMPILE", "PASSED", "compile ok", 0, "2026-07-11T00:00:02Z")
                )
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> statements(Object body) {
        return (List<Map<String, Object>>) map(body).get("statements");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parameters(Map<String, Object> statement) {
        return (Map<String, Object>) statement.get("parameters");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static final class CapturingHttpClient extends JsonHttpClient {
        private String url;
        private final List<Object> bodies = new ArrayList<>();
        private Map<String, String> headers;
        private JsonNode response = JsonNodeFactory.instance.objectNode();

        @Override
        public JsonNode post(String url, Object body, Map<String, String> headers) {
            this.url = url;
            this.bodies.add(body);
            this.headers = headers;
            return response;
        }
    }

    private JsonNode responseWithError(String code, String message) {
        var root = JsonNodeFactory.instance.objectNode();
        ArrayNode errors = root.putArray("errors");
        errors.addObject().put("code", code).put("message", message);
        return root;
    }

    private static class EnabledConfig implements Neo4jRuntimeConfig {
        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public String httpUrl() {
            return "http://localhost:7474";
        }

        @Override
        public String database() {
            return "neo4j";
        }

        @Override
        public String username() {
            return "neo4j";
        }

        @Override
        public String password() {
            return "password";
        }
    }

    private static final class DisabledConfig extends EnabledConfig {
        @Override
        public boolean enabled() {
            return false;
        }
    }
}
