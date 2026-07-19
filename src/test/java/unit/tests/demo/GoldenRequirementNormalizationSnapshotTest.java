package unit.tests.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.RuntimeProperties;
import ua.demo.agentlab.demo.snapshot.GoldenRequirementSnapshot;
import ua.demo.agentlab.demo.snapshot.GoldenRequirementSnapshotArtifactWriter;
import ua.demo.agentlab.demo.snapshot.GoldenRequirementSnapshotBuilder;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.source.FileRequirementSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class GoldenRequirementNormalizationSnapshotTest {

    private static final String PROFILE = "demo/orangehrm-login-logout/project-profile.yaml";
    private static final String REQUIREMENTS = "demo/orangehrm-login-logout/requirements.md";
    private static final Path EXPECTED = Path.of("demo", "orangehrm-login-logout", "expected");
    private static final Path ACTUAL = Path.of("target", "test-artifacts", "bw-02");
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    public void repeatedExecutionProducesIdenticalNormalizedSnapshot() throws IOException {
        GoldenRequirementSnapshot first = snapshot();
        GoldenRequirementSnapshot second = snapshot();

        Assert.assertEquals(second, first, "Repeated normalization must be value-identical");
        Assert.assertEquals(objectMapper.writeValueAsString(second), objectMapper.writeValueAsString(first),
                "Repeated normalization must serialize identically");
    }

    @Test
    public void executableRequirementsHaveCompleteTypedOwnershipAndGovernanceIsSeparate() {
        GoldenRequirementSnapshot snapshot = snapshot();
        List<GoldenRequirementSnapshot.NormalizedRequirementSnapshot> executable = snapshot
                .normalizedRequirementBundle().requirements().stream()
                .filter(GoldenRequirementSnapshot.NormalizedRequirementSnapshot::executable)
                .toList();

        Assert.assertEquals(executable.size(), 4);
        executable.forEach(requirement -> {
            Assert.assertFalse(requirement.capability().isBlank(), requirement.id() + " capability is missing");
            Assert.assertFalse(requirement.actions().isEmpty(), requirement.id() + " actions are missing");
            Assert.assertFalse(requirement.sourceContext().page().isBlank(), requirement.id() + " source page is missing");
            Assert.assertFalse(requirement.sourceContext().route().isBlank(), requirement.id() + " source route is missing");
            Assert.assertFalse(requirement.targetContext().page().isBlank(), requirement.id() + " target page is missing");
            Assert.assertFalse(requirement.targetContext().route().isBlank(), requirement.id() + " target route is missing");
            Assert.assertFalse(requirement.assertions().isEmpty(), requirement.id() + " typed assertions are missing");
            requirement.assertions().forEach(assertion -> {
                Assert.assertFalse(assertion.type().isBlank(), requirement.id() + " assertion type is missing");
                Assert.assertFalse(assertion.expectedValue().isBlank(),
                        requirement.id() + " assertion expectedValue is missing");
            });
        });

        Set<String> scenarioIds = snapshot.canonicalTestCaseBundle().scenarios().stream()
                .map(GoldenRequirementSnapshot.CanonicalScenarioSnapshot::id)
                .collect(Collectors.toSet());
        Set<String> governanceIds = snapshot.governanceRequirementBundle().requirements().stream()
                .map(GoldenRequirementSnapshot.GovernanceRequirementSnapshot::requirementId)
                .collect(Collectors.toSet());
        Assert.assertEquals(scenarioIds, Set.of("REQ-001", "REQ-002", "REQ-003", "REQ-004"));
        Assert.assertEquals(governanceIds, Set.of("GOV-001", "GOV-002", "GOV-003"));
        Assert.assertTrue(scenarioIds.stream().noneMatch(governanceIds::contains));

        snapshot.canonicalTestCaseBundle().scenarios().forEach(scenario -> {
            scenario.assertions().forEach(assertion -> {
                Assert.assertFalse(assertion.expectedValue().isBlank(),
                        scenario.id() + " canonical assertion expectedValue is missing");
                Assert.assertEquals(assertion.ownerPage(), scenario.targetContext().page(),
                        scenario.id() + " assertion has cross-page ownership");
                Assert.assertEquals(assertion.route(), scenario.targetContext().route(),
                        scenario.id() + " assertion has cross-route ownership");
            });
            scenario.operations().forEach(operation -> Assert.assertTrue(
                    operation.setup()
                            || operation.ownerPage().equals(scenario.sourceContext().page())
                            || operation.ownerPage().equals(scenario.targetContext().page()),
                    scenario.id() + " operation owner is outside source/target context: " + operation.ownerPage()
            ));
            scenario.operations().forEach(operation -> {
                Assert.assertFalse(operation.ownerPage().isBlank(), scenario.id() + " operation owner is missing");
                Assert.assertFalse(operation.route().isBlank(), scenario.id() + " operation route is missing");
            });
        });

        GoldenRequirementSnapshot.CanonicalScenarioSnapshot authentication = scenario(snapshot, "REQ-002");
        Assert.assertEquals(authentication.sourceContext(),
                new GoldenRequirementSnapshot.PageContextSnapshot("LoginPage", "/auth/login"));
        Assert.assertEquals(authentication.targetContext(),
                new GoldenRequirementSnapshot.PageContextSnapshot("DashboardPage", "/dashboard/index"));

        GoldenRequirementSnapshot.CanonicalScenarioSnapshot logout = scenario(snapshot, "REQ-004");
        Assert.assertEquals(logout.sourceContext(),
                new GoldenRequirementSnapshot.PageContextSnapshot("DashboardPage", "/dashboard/index"));
        Assert.assertEquals(logout.targetContext(),
                new GoldenRequirementSnapshot.PageContextSnapshot("LoginPage", "/auth/login"));
        Assert.assertTrue(logout.operations().stream().anyMatch(operation -> operation.kind().equals("LOGOUT")
                && operation.ownerPage().equals("DashboardPage")));
        Assert.assertEquals(logout.operations().stream().map(GoldenRequirementSnapshot.OperationSnapshot::kind).toList(),
                List.of("AUTHENTICATE", "OPEN_MENU", "LOGOUT"));
        Assert.assertTrue(logout.operations().stream().anyMatch(operation -> operation.kind().equals("OPEN_MENU")
                        && operation.ownerPage().equals("DashboardPage")
                        && operation.dataKey().equals("userMenu")
                        && operation.setup()),
                "Menu-mediated logout must recreate the user-menu-open prerequisite");
    }

    @Test
    public void generatedArtifactsMatchVersionedGoldenFiles() throws IOException {
        GoldenRequirementSnapshot snapshot = snapshot();
        new GoldenRequirementSnapshotArtifactWriter().write(ACTUAL, snapshot);

        assertJsonEquals("normalized-requirements.json");
        assertJsonEquals("structured-behavior-contracts.json");
        assertJsonEquals("canonical-test-cases.json");
        assertJsonEquals("governance-requirements.json");
    }

    private GoldenRequirementSnapshot snapshot() {
        RequirementDocument document = new FileRequirementSource().load(
                new RequirementInput(SourceType.FILE, REQUIREMENTS)
        );
        Properties properties = new Properties();
        properties.setProperty("project.profile.file", PROFILE);
        ProjectProfile profile = new PropertiesProjectProfileLoader(new RuntimeProperties(properties))
                .loadDefaultProfile();
        return new GoldenRequirementSnapshotBuilder().build(document, profile);
    }

    private GoldenRequirementSnapshot.CanonicalScenarioSnapshot scenario(
            GoldenRequirementSnapshot snapshot,
            String id
    ) {
        return snapshot.canonicalTestCaseBundle().scenarios().stream()
                .filter(scenario -> scenario.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing canonical scenario " + id));
    }

    private void assertJsonEquals(String fileName) throws IOException {
        Path expected = EXPECTED.resolve(fileName);
        Path actual = ACTUAL.resolve(fileName);
        Assert.assertTrue(Files.isRegularFile(expected), "Missing golden file: " + expected);
        JsonNode expectedJson = objectMapper.readTree(expected.toFile());
        JsonNode actualJson = objectMapper.readTree(actual.toFile());
        Assert.assertEquals(actualJson, expectedJson,
                "Golden snapshot differs for " + fileName + "; inspect " + actual);
    }
}
