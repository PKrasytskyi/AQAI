package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.persistence.CanonicalInteractionGraphProjectionWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

public class CanonicalInteractionGraphProjectionWriterTest {

    @Test
    public void reportsDisabledDatabaseWithoutAttemptingNetworkCall() {
        Neo4jRuntimeConfig disabled = new Neo4jRuntimeConfig() {
            public boolean enabled() { return false; }
            public String httpUrl() { return "http://localhost:7474"; }
            public String database() { return "neo4j"; }
            public String username() { return "neo4j"; }
            public String password() { return ""; }
        };
        var result = new CanonicalInteractionGraphProjectionWriter(disabled).persist(null, null);
        Assert.assertFalse(result.executed());
        Assert.assertTrue(result.details().contains("empty"));
    }
}
