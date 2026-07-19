package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.SpaStateGraphWriter;
import ua.demo.agentlab.ui.discovery.spa.model.SpaStateGraph;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.util.List;

public class SpaStateGraphWriterTest {

    @Test
    public void keepsLiveStateGraphTypedAndSkipsPersistenceWhenNeo4jIsDisabled() {
        KnowledgeRunMetadata metadata = new KnowledgeRunMetadata("run", "app", "base", "requirements", "session",
                "ui-knowledge-v2", "now", "test", 1.0d);
        UiStateSnapshot login = new UiStateSnapshot("state-login", "login", "/auth/login", "fingerprint-login", metadata,
                List.of("login-form"), List.of(), List.of(), List.of(), false, true, false, 0.95d, List.of("test"));
        UiStateSnapshot dashboard = new UiStateSnapshot("state-dashboard", "dashboard", "/dashboard/index", "fingerprint-dashboard", metadata,
                List.of("header"), List.of(), List.of(), List.of(), false, true, true, 0.95d, List.of("test"));
        SpaStateGraph graph = new SpaStateGraph(SpaStateGraph.SCHEMA_VERSION, metadata, List.of(login, dashboard),
                List.of(new UiStateTransition("transition-login", login.stateId(), dashboard.stateId(), "login", "AUTHENTICATE",
                        "route contains /dashboard/index", true, false, 0.98d, metadata, List.of("test"))), List.of("test"));

        Neo4jRuntimeConfig disabled = new Neo4jRuntimeConfig() {
            @Override public boolean enabled() { return false; }
            @Override public String httpUrl() { return "http://localhost:7474"; }
            @Override public String database() { return "neo4j"; }
            @Override public String username() { return "neo4j"; }
            @Override public String password() { return ""; }
        };

        String result = new SpaStateGraphWriter(disabled).persist(graph);

        Assert.assertEquals(graph.states().size(), 2);
        Assert.assertEquals(graph.transitions().get(0).actionId(), "login");
        Assert.assertEquals(result, "skipped:neo4j-disabled");
    }
}
