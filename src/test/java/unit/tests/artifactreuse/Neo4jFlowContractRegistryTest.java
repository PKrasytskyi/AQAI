package unit.tests.artifactreuse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.artifactreuse.flow.FlowActionType;
import ua.demo.agentlab.artifactreuse.flow.FlowContract;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBundle;
import ua.demo.agentlab.artifactreuse.flow.FlowContractPersistenceResult;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStatus;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStep;
import ua.demo.agentlab.artifactreuse.flow.FlowContractType;
import ua.demo.agentlab.artifactreuse.flow.FlowEndpoint;
import ua.demo.agentlab.artifactreuse.flow.FlowState;
import ua.demo.agentlab.artifactreuse.flow.Neo4jFlowContractRegistry;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class Neo4jFlowContractRegistryTest {

    @Test
    public void persistsFlowGraphWithStatesStepsAndArtifactRelationship() {
        CapturingHttpClient http = new CapturingHttpClient();
        Neo4jFlowContractRegistry registry = new Neo4jFlowContractRegistry(new EnabledConfig(), http);
        FlowContractPersistenceResult result = registry.persist(bundle());

        Assert.assertTrue(result.success());
        Assert.assertEquals(result.contractsPersisted(), 1);
        Assert.assertEquals(http.url, "http://localhost:7474/db/neo4j/tx/commit");
        Assert.assertEquals(http.bodies.size(), 2, "Schema and data writes must use separate Neo4j transactions");
        List<Map<String, Object>> statements = statements(http.bodies.get(1));
        String graphStatement = statements.get(0).get("statement").toString();
        Assert.assertTrue(graphStatement.contains("[:STARTS_AT]"));
        Assert.assertTrue(graphStatement.contains("[:ENDS_AT]"));
        Assert.assertTrue(graphStatement.contains("[:REQUIRES_STATE]"));
        Assert.assertTrue(graphStatement.contains("[:PRODUCES_STATE]"));
        Assert.assertTrue(graphStatement.contains("[:USES_ARTIFACT]"));
        Assert.assertTrue(graphStatement.contains("contractFingerprint"));
        Assert.assertTrue(graphStatement.contains("runtimePassRate"));
    }

    @Test
    public void persistsRuntimeFeedbackCountersAndQualityRates() {
        CapturingHttpClient http = new CapturingHttpClient();
        http.response = responseWithUpdatedCount(1);
        Neo4jFlowContractRegistry registry = new Neo4jFlowContractRegistry(new EnabledConfig(), http);

        var result = registry.recordRuntimeFeedback(bundle(), new ua.demo.agentlab.artifactreuse.flow.FlowRuntimeFeedback(
                true, "live-ui-smoke", "2026-07-12T09:00:00Z"));

        Assert.assertTrue(result.success());
        String statement = statements(http.bodies.get(0)).get(0).get("statement").toString();
        Assert.assertTrue(statement.contains("runtimeSmokeAttempts"));
        Assert.assertTrue(statement.contains("lastSuccessfulSmoke"));
        Assert.assertTrue(statement.contains("lastRuntimeSmokeStatus"));
        Assert.assertTrue(statement.contains("flakyRate"));
    }

    @Test
    public void reportsNeo4jTransactionErrorsInsteadOfPretendingPersistenceSucceeded() {
        CapturingHttpClient http = new CapturingHttpClient();
        http.response = responseWithError("Neo.ClientError.Statement.SyntaxError", "bad cypher");
        Neo4jFlowContractRegistry registry = new Neo4jFlowContractRegistry(new EnabledConfig(), http);

        FlowContractPersistenceResult result = registry.persist(bundle());

        Assert.assertFalse(result.success());
        Assert.assertTrue(result.message().contains("Neo.ClientError.Statement.SyntaxError"));
    }

    @Test
    public void reportsEmptyExactLookupWithAVisibleDiagnostic() {
        CapturingHttpClient http = new CapturingHttpClient();
        Neo4jFlowContractRegistry registry = new Neo4jFlowContractRegistry(new EnabledConfig(), http);

        var result = registry.findConfirmedByIdsWithResult(List.of("AUTHENTICATION_LOGIN_TO_DASHBOARD"),
                new ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata(
                        "run", "app", "base", "requirements", "session", "ui-knowledge-v2", "now", "test", 1.0d));

        Assert.assertTrue(result.success());
        Assert.assertTrue(result.contracts().isEmpty());
        Assert.assertTrue(result.message().contains("No confirmed Neo4j flow"));
    }

    @Test
    public void reportsRuntimeFeedbackFailureWhenNoPersistedFlowWasUpdated() {
        CapturingHttpClient http = new CapturingHttpClient();
        http.response = responseWithUpdatedCount(0);
        Neo4jFlowContractRegistry registry = new Neo4jFlowContractRegistry(new EnabledConfig(), http);

        var result = registry.recordRuntimeFeedback(bundle(), new ua.demo.agentlab.artifactreuse.flow.FlowRuntimeFeedback(
                true, "live-ui-smoke", "2026-07-12T09:00:00Z"));

        Assert.assertFalse(result.success());
        Assert.assertTrue(result.message().contains("No persisted Neo4j flow contracts"));
    }

    private FlowContractBundle bundle() {
        return new FlowContractBundle("flow-contract-bundle.v1", null, List.of(new FlowContract(
                "flow-contract.v1", "AUTHENTICATION_LOGIN_TO_DASHBOARD", "Authentication", FlowContractType.AUTHENTICATION,
                new FlowEndpoint("LoginPage", "/login"), new FlowEndpoint("DashboardPage", "/dashboard"),
                List.of(FlowState.UNAUTHENTICATED), List.of(FlowState.AUTHENTICATED),
                List.of(new FlowContractStep(1, FlowActionType.AUTHENTICATE, "LoginPage", "/login", "", false)),
                List.of("dashboard route visible"), List.of("REQ-1"), List.of("requirement:REQ-1", "transition:/login->/dashboard"),
                List.of("pom-contract:LoginPage:abc"), 0.95d, FlowContractStatus.CONFIRMED
        )));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> statements(Object body) {
        return (List<Map<String, Object>>) ((Map<String, Object>) body).get("statements");
    }

    private static final class CapturingHttpClient extends JsonHttpClient {
        private String url;
        private final List<Object> bodies = new ArrayList<>();
        private JsonNode response = JsonNodeFactory.instance.objectNode();

        @Override
        public JsonNode post(String url, Object body, Map<String, String> headers) {
            this.url = url;
            this.bodies.add(body);
            return response;
        }
    }

    private JsonNode responseWithError(String code, String message) {
        var root = JsonNodeFactory.instance.objectNode();
        ArrayNode errors = root.putArray("errors");
        errors.addObject().put("code", code).put("message", message);
        return root;
    }

    private JsonNode responseWithUpdatedCount(int updated) {
        var root = JsonNodeFactory.instance.objectNode();
        var results = root.putArray("results");
        var data = results.addObject().putArray("data");
        data.addObject().putArray("row").add(updated);
        root.putArray("errors");
        return root;
    }

    private static final class EnabledConfig implements Neo4jRuntimeConfig {
        @Override public boolean enabled() { return true; }
        @Override public String httpUrl() { return "http://localhost:7474"; }
        @Override public String database() { return "neo4j"; }
        @Override public String username() { return "neo4j"; }
        @Override public String password() { return "password"; }
    }
}
