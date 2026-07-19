package unit.tests.demo;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.RuntimeProperties;
import ua.demo.agentlab.demo.DemoManifest;
import ua.demo.agentlab.demo.DemoManifestLoader;
import ua.demo.agentlab.demo.DemoManifestPreflightService;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.source.FileRequirementSource;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseInput;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.testcase.planning.ScenarioPipelineRequirementToTestCaseGenerator;
import ua.demo.agentlab.ui.capability.LogoutAccessMode;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CrossProductAuthenticationAcceptanceTest {

    private static final Path WORKSPACE = Path.of("").toAbsolutePath().normalize();

    @DataProvider
    public Object[][] productFixtures() {
        return new Object[][]{
                {
                        "demo/orangehrm-login-logout/demo-manifest.yaml",
                        LogoutAccessMode.USER_MENU,
                        "/auth/login",
                        "/dashboard/index",
                        "LoginPage",
                        "DashboardPage"
                },
                {
                        "demo/the-internet-authentication-logout/demo-manifest.yaml",
                        LogoutAccessMode.DIRECT_CONTROL,
                        "/login",
                        "/secure",
                        "LoginPage",
                        "SecureAreaPage"
                }
        };
    }

    @Test(dataProvider = "productFixtures")
    public void realProfilesAndFixturesResolveTheSharedAuthenticationLifecycle(
            String manifestPath,
            LogoutAccessMode accessMode,
            String loginRoute,
            String authenticatedRoute,
            String loginPage,
            String authenticatedPage
    ) {
        ResolvedFixture fixture = resolve(manifestPath);

        Assert.assertTrue(fixture.preflightReady(), fixture.preflightIssues());
        Assert.assertEquals(fixture.profile().loginRoute(), loginRoute);
        Assert.assertEquals(fixture.profile().authenticatedRoute(), authenticatedRoute);
        Assert.assertEquals(fixture.manifest().expectedLogoutAccessMode(), accessMode);
        Assert.assertEquals(fixture.manifest().expectedLifecycle(), accessMode.lifecycle());
        Assert.assertEquals(fixture.canonical().testCases().size(), 4);
        Assert.assertEquals(fixture.canonical().testCases().stream().map(CanonicalTestCase::id).toList(),
                List.of("REQ-001", "REQ-002", "REQ-003", "REQ-004"));

        CanonicalTestCase authentication = scenario(fixture, "REQ-002");
        Assert.assertEquals(authentication.sourcePageName(), loginPage);
        Assert.assertEquals(authentication.sourceRoute(), loginRoute);
        Assert.assertEquals(authentication.pageName(), authenticatedPage);
        Assert.assertEquals(authentication.route(), authenticatedRoute);

        CanonicalTestCase logout = scenario(fixture, "REQ-004");
        Assert.assertEquals(logout.sourcePageName(), authenticatedPage);
        Assert.assertEquals(logout.sourceRoute(), authenticatedRoute);
        Assert.assertEquals(logout.pageName(), loginPage);
        Assert.assertEquals(logout.route(), loginRoute);
        Assert.assertTrue(logout.assertionIntents().stream()
                .noneMatch(assertion -> assertion.expectedValue() == null || assertion.expectedValue().isBlank()));

        if (accessMode == LogoutAccessMode.USER_MENU) {
            assertOperationKinds(scenario(fixture, "REQ-003"), "AUTHENTICATE", "OPEN_MENU");
            assertOperationKinds(logout, "AUTHENTICATE", "OPEN_MENU", "LOGOUT");
            Assert.assertEquals(logout.operationIntents().get(1).dataKey(), "userMenu");
        } else {
            assertOperationKinds(scenario(fixture, "REQ-003"), "AUTHENTICATE", "INSPECT_PAGE_CONTENT");
            assertOperationKinds(logout, "AUTHENTICATE", "LOGOUT");
            Assert.assertTrue(fixture.canonical().testCases().stream()
                    .flatMap(testCase -> testCase.operationIntents().stream())
                    .noneMatch(operation -> operation.kind().name().equals("OPEN_MENU")
                            || "userMenu".equals(operation.dataKey())));
        }
    }

    @Test
    public void switchingOnlyProfileAndRequirementsDoesNotLeakProductTopology() throws IOException {
        ResolvedFixture orangeHrm = resolve("demo/orangehrm-login-logout/demo-manifest.yaml");
        ResolvedFixture theInternet = resolve("demo/the-internet-authentication-logout/demo-manifest.yaml");

        Assert.assertEquals(orangeHrm.manifest().schemaVersions(), theInternet.manifest().schemaVersions(),
                "Both products must use the same contract schemas");
        Assert.assertNotEquals(orangeHrm.profile().baseUrl(), theInternet.profile().baseUrl());
        Assert.assertNotEquals(orangeHrm.profile().authenticatedRoute(), theInternet.profile().authenticatedRoute());
        Assert.assertNotEquals(scenario(orangeHrm, "REQ-002").pageName(), scenario(theInternet, "REQ-002").pageName());

        String internetArtifacts = artifactText(theInternet);
        Assert.assertFalse(internetArtifacts.contains("/auth/login"));
        Assert.assertFalse(internetArtifacts.contains("/dashboard/index"));
        Assert.assertFalse(internetArtifacts.contains("DashboardPage"));
        Assert.assertFalse(internetArtifacts.contains("USER_MENU"));
        Assert.assertFalse(internetArtifacts.contains("userMenu"));

        String internetProfile = Files.readString(
                WORKSPACE.resolve(theInternet.manifest().projectProfilePath())).toLowerCase(java.util.Locale.ROOT);
        Assert.assertFalse(internetProfile.contains("orangehrm"));
        Assert.assertFalse(internetProfile.contains("oxd-"));
        Assert.assertFalse(internetProfile.contains("/auth/login"));
        Assert.assertFalse(internetProfile.contains("/dashboard/index"));

        String orangeArtifacts = artifactText(orangeHrm);
        Assert.assertFalse(orangeArtifacts.contains("DIRECT_CONTROL"));
        Assert.assertFalse(orangeArtifacts.contains("DIRECT_LOGOUT_CONTROL"));
    }

    private ResolvedFixture resolve(String manifestPath) {
        DemoManifest manifest = new DemoManifestLoader().load(WORKSPACE.resolve(manifestPath));
        var preflight = new DemoManifestPreflightService().validate(
                manifest,
                WORKSPACE,
                Map.of(
                        "TEST_VALID_USERNAME", "configured-user",
                        "TEST_VALID_PASSWORD", "configured-password",
                        "OPENAI_API_KEY", "configured-openai-key",
                        "KNOWLEDGE_DB_STATUS", "false"
                )::get
        );
        ProjectProfile profile = profile(manifest.projectProfilePath());
        NormalizedRequirementBundle normalized = new RuleBasedRequirementNormalizer().normalize(
                new FileRequirementSource().load(new RequirementInput(
                        SourceType.FILE,
                        manifest.requirementFixturePath()
                ))
        );
        CanonicalTestCaseBundle canonical = ScenarioPipelineRequirementToTestCaseGenerator.deterministic().generate(
                new RequirementToTestCaseInput(profile, normalized, null, null)
        );
        return new ResolvedFixture(
                manifest,
                profile,
                normalized,
                canonical,
                preflight.ready(),
                preflight.issues().toString()
        );
    }

    private ProjectProfile profile(String profilePath) {
        Properties properties = new Properties();
        properties.setProperty("project.profile.file", profilePath);
        return new PropertiesProjectProfileLoader(new RuntimeProperties(properties)).loadDefaultProfile();
    }

    private CanonicalTestCase scenario(ResolvedFixture fixture, String id) {
        return fixture.canonical().testCases().stream()
                .filter(testCase -> testCase.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing canonical scenario " + id));
    }

    private void assertOperationKinds(CanonicalTestCase scenario, String... expectedKinds) {
        Assert.assertEquals(
                scenario.operationIntents().stream().map(operation -> operation.kind().name()).toList(),
                List.of(expectedKinds),
                scenario.id() + " operation sequence"
        );
    }

    private String artifactText(ResolvedFixture fixture) {
        return fixture.manifest().expectedLifecycle() + " "
                + fixture.normalized().requirements() + " "
                + fixture.canonical().testCases();
    }

    private record ResolvedFixture(
            DemoManifest manifest,
            ProjectProfile profile,
            NormalizedRequirementBundle normalized,
            CanonicalTestCaseBundle canonical,
            boolean preflightReady,
            String preflightIssues
    ) {
    }
}
