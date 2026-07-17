package unit.tests.demo;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.demo.DemoManifest;
import ua.demo.agentlab.demo.DemoManifestLoader;
import ua.demo.agentlab.demo.DemoManifestPreflightService;

import java.nio.file.Path;
import java.util.Map;

public class DemoManifestPreflightServiceTest {

    private static final Path WORKSPACE = Path.of("").toAbsolutePath().normalize();
    private static final Path MANIFEST = WORKSPACE.resolve(
            "demo/orangehrm-login-logout/demo-manifest.yaml");

    @Test
    public void orangeHrmDemoBundlePassesWithRequiredEnvironment() {
        DemoManifest manifest = new DemoManifestLoader().load(MANIFEST);
        Map<String, String> environment = Map.of(
                "TEST_VALID_USERNAME", "configured-user",
                "TEST_VALID_PASSWORD", "configured-password",
                "OPENAI_API_KEY", "configured-openai-key",
                "KNOWLEDGE_DB_STATUS", "false"
        );

        var report = new DemoManifestPreflightService().validate(
                manifest, WORKSPACE, environment::get);

        Assert.assertTrue(report.ready(), report.issues().toString());
        Assert.assertTrue(report.issues().isEmpty());
    }

    @Test
    public void missingCredentialsProduceOneExplicitPreflightFailure() {
        DemoManifest manifest = new DemoManifestLoader().load(MANIFEST);

        var report = new DemoManifestPreflightService().validate(
                manifest, WORKSPACE, ignored -> null);

        Assert.assertFalse(report.ready());
        Assert.assertEquals(report.issues().size(), 1);
        Assert.assertEquals(report.issues().get(0).code(), "MISSING_REQUIRED_ENVIRONMENT");
        Assert.assertTrue(report.issues().get(0).message().contains("TEST_VALID_USERNAME"));
        Assert.assertTrue(report.issues().get(0).message().contains("TEST_VALID_PASSWORD"));
        Assert.assertFalse(report.issues().get(0).message().contains("configured-password"));
    }

    @Test
    public void withDbOverrideIsRejectedForBaselineManifest() {
        DemoManifest manifest = new DemoManifestLoader().load(MANIFEST);
        Map<String, String> environment = Map.of(
                "TEST_VALID_USERNAME", "configured-user",
                "TEST_VALID_PASSWORD", "configured-password",
                "OPENAI_API_KEY", "configured-openai-key",
                "KNOWLEDGE_DB_STATUS", "true"
        );

        var report = new DemoManifestPreflightService().validate(
                manifest, WORKSPACE, environment::get);

        Assert.assertFalse(report.ready());
        Assert.assertEquals(report.issues().size(), 1);
        Assert.assertEquals(report.issues().get(0).code(), "DATABASE_MODE_MISMATCH");
    }
}
