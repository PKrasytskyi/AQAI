package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.SpaEvidenceLifecycleGraphWriter;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

public class SpaInventoryGraphWriterTest {

    @Test
    public void skipsPersistenceWhenGraphKnowledgeIsDisabled() {
        Neo4jRuntimeConfig disabled = new Neo4jRuntimeConfig() {
            @Override public boolean enabled() { return false; }
            @Override public String httpUrl() { return "http://localhost:7474"; }
            @Override public String database() { return "neo4j"; }
            @Override public String username() { return "neo4j"; }
            @Override public String password() { return ""; }
        };

        var result = new SpaEvidenceLifecycleGraphWriter(disabled).update(
                new SpaTargetedVerificationResult(
                        SpaTargetedVerificationResult.SCHEMA_VERSION,
                        new KnowledgeRunMetadata("run", "app", "base", "requirements", "session", "ui-knowledge-v2", "now", "test", 1.0d),
                        List.of(new TargetedLocatorVerification("login", "/login", "fingerprint", "form", "username",
                                "username", "name", "username", 0.9d, true, "verified", List.of("REQ-1"))),
                        List.of(), List.of(), List.of("test")
                ),
                new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30, true, 0.8d, 2, 2)
        );

        Assert.assertFalse(result.executed());
        Assert.assertTrue(result.details().contains("disabled"));
    }
}
