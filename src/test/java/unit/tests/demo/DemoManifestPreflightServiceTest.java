package unit.tests.demo;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.demo.DemoDatabaseMode;
import ua.demo.agentlab.demo.DemoManifest;
import ua.demo.agentlab.demo.DemoManifestLoader;
import ua.demo.agentlab.demo.DemoManifestPreflightService;

import java.nio.file.Path;
import java.util.Map;

public class DemoManifestPreflightServiceTest {

    private static final Path WORKSPACE = Path.of("").toAbsolutePath().normalize();
    private static final Path MANIFEST = WORKSPACE.resolve(
            "demo/orangehrm-login-logout/demo-manifest.yaml");
    private static final Path THE_INTERNET_MANIFEST = WORKSPACE.resolve(
            "demo/the-internet-authentication-logout/demo-manifest.yaml");

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
        Assert.assertEquals(report.resolution().profileId(), "orangeHRM");
        Assert.assertEquals(report.resolution().loginRoute(), "/auth/login");
        Assert.assertEquals(report.resolution().authenticatedRoute(), "/dashboard/index");
        Assert.assertEquals(report.resolution().logoutAccessMode().name(), "USER_MENU");
        Assert.assertEquals(report.resolution().lifecycle(),
                java.util.List.of("AUTHENTICATION", "AUTHENTICATED_AREA", "USER_MENU", "LOGOUT"));
    }

    @Test
    public void theInternetDemoBundlePassesWithDirectLogoutTopology() {
        DemoManifest manifest = new DemoManifestLoader().load(THE_INTERNET_MANIFEST);
        Map<String, String> environment = configuredEnvironment("false");

        var report = new DemoManifestPreflightService().validate(
                manifest, WORKSPACE, environment::get);

        Assert.assertTrue(report.ready(), report.issues().toString());
        Assert.assertTrue(report.issues().isEmpty());
        Assert.assertEquals(report.resolution().profileId(), "the-internet");
        Assert.assertEquals(report.resolution().homeRoute(), "/");
        Assert.assertEquals(report.resolution().loginRoute(), "/login");
        Assert.assertEquals(report.resolution().authenticatedRoute(), "/secure");
        Assert.assertEquals(report.resolution().logoutAccessMode().name(), "DIRECT_CONTROL");
        Assert.assertEquals(report.resolution().lifecycle(), java.util.List.of(
                "AUTHENTICATION", "AUTHENTICATED_AREA", "DIRECT_LOGOUT_CONTROL", "LOGOUT"));
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
    public void environmentControlledManifestAcceptsWithDbRun() {
        DemoManifest manifest = new DemoManifestLoader().load(MANIFEST);
        Map<String, String> environment = Map.of(
                "TEST_VALID_USERNAME", "configured-user",
                "TEST_VALID_PASSWORD", "configured-password",
                "OPENAI_API_KEY", "configured-openai-key",
                "KNOWLEDGE_DB_STATUS", "true"
        );

        var report = new DemoManifestPreflightService().validate(
                manifest, WORKSPACE, environment::get);

        Assert.assertTrue(report.ready(), report.issues().toString());
        Assert.assertTrue(report.issues().isEmpty());
    }

    @Test
    public void fixedWithoutDbBaselineStillRejectsWithDbRun() {
        DemoManifest manifest = withDatabaseMode(
                new DemoManifestLoader().load(MANIFEST),
                DemoDatabaseMode.WITHOUT_DB_BASELINE
        );

        var report = new DemoManifestPreflightService().validate(
                manifest, WORKSPACE, configuredEnvironment("true")::get);

        Assert.assertFalse(report.ready());
        Assert.assertEquals(report.issues().size(), 1);
        Assert.assertEquals(report.issues().get(0).code(), "DATABASE_MODE_MISMATCH");
    }

    @Test
    public void invalidDatabaseStatusIsReportedExplicitly() {
        DemoManifest manifest = new DemoManifestLoader().load(MANIFEST);

        var report = new DemoManifestPreflightService().validate(
                manifest, WORKSPACE, configuredEnvironment("enabled")::get);

        Assert.assertFalse(report.ready());
        Assert.assertEquals(report.issues().size(), 1);
        Assert.assertEquals(report.issues().get(0).code(), "DATABASE_STATUS_INVALID");
    }

    private Map<String, String> configuredEnvironment(String dbStatus) {
        return Map.of(
                "TEST_VALID_USERNAME", "configured-user",
                "TEST_VALID_PASSWORD", "configured-password",
                "OPENAI_API_KEY", "configured-openai-key",
                "KNOWLEDGE_DB_STATUS", dbStatus
        );
    }

    private DemoManifest withDatabaseMode(DemoManifest manifest, DemoDatabaseMode databaseMode) {
        return new DemoManifest(
                manifest.schemaVersion(),
                manifest.demoId(),
                manifest.projectProfilePath(),
                manifest.requirementFixturePath(),
                manifest.requiredEnvironmentVariables(),
                manifest.expectedPageCapabilities(),
                manifest.expectedScenarioIds(),
                manifest.expectedPomNames(),
                manifest.expectedLogoutAccessMode(),
                manifest.expectedLifecycle(),
                manifest.expectedFinalRoute(),
                manifest.expectedFinalState(),
                databaseMode,
                manifest.aiMode(),
                manifest.schemaVersions()
        );
    }
}
