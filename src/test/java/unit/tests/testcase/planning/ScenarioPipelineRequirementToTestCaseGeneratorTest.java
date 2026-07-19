package unit.tests.testcase.planning;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ai.assertions.service.AssertionContractBuilder;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseInput;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.testcase.planning.ScenarioPipelineRequirementToTestCaseGenerator;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public class ScenarioPipelineRequirementToTestCaseGeneratorTest {

    @Test
    public void preservesExplicitTargetRouteAndAllStructuredAssertions() {
        String source = """
                ## Requirement: REQ-001 Enter Numeric Value
                ### Capability
                `FORM`
                ### Action
                * Enter `42` into the input field.
                ### Expected Result
                * The field keeps the entered value.
                ### Assertion Requirements
                * `type: INPUT_VALUE_MATCHES`
                  * `target: numericInput`
                  * `expectedValue: 42`
                * `type: ELEMENT_VISIBLE`
                  * `target: numericInput`
                  * `expectedValue: Numeric input is visible`
                ### Target Context
                * `pageCapability: FORM`
                * `componentCapability: FORM, INPUT`
                * `targetRoute: /inputs`
                * `targetPage: Inputs`
                ### Data Requirements
                * No scenario data is required.
                """;
        NormalizedRequirementBundle requirements = new RuleBasedRequirementNormalizer()
                .normalize(new RequirementDocument("requirements/inputs.md", source));
        ProjectProfile profile = new ProjectProfile(
                "demo", "Demo App", "https://example.test", "/", "/login", "", "/secure",
                "", "", "", "", "", "", "", new OutputProfile("pages", "tests")
        );
        MappedUiKnowledge knowledge = new MappedUiKnowledge(List.of(new ua.demo.agentlab.ui.discovery.mapping.model.MappedPage(
                "inputs", "InputsPage", "form", "https://example.test/inputs", "/inputs", "Inputs",
                List.of(), List.of(), List.of(), List.of(), List.of(), null, "", ""
        )), List.of(), List.of(), List.of(), List.of());

        CanonicalTestCaseBundle bundle = new ScenarioPipelineRequirementToTestCaseGenerator().generate(
                new RequirementToTestCaseInput(profile, requirements, knowledge, null)
        );

        var testCase = bundle.testCases().get(0);
        Assert.assertEquals(testCase.pageName(), "InputsPage");
        Assert.assertEquals(testCase.route(), "/inputs");
        Assert.assertEquals(testCase.assertionIntents().size(), 2);
        Assert.assertEquals(testCase.assertionIntents().get(0).kind().name(), "INPUT_VALUE_MATCHES");
        Assert.assertEquals(testCase.assertionIntents().get(0).target(), "numericInput");
        var contracts = new AssertionContractBuilder().build(List.of(testCase));
        Assert.assertEquals(contracts.stream().map(contract -> contract.type()).toList(),
                List.of(AssertionType.INPUT_VALUE_MATCHES, AssertionType.ELEMENT_VISIBLE));
        Assert.assertTrue(contracts.stream().allMatch(contract -> contract.route().equals("/inputs")));
    }

    @Test
    public void doesNotInventRecordDetailsRouteWhenProfileDiscoveryAndCacheDoNotConfirmIt() {
        ProjectProfile profile = new ProjectProfile(
                "demo",
                "Demo App",
                "https://example.test",
                "/",
                "/auth/login",
                "",
                "/dashboard/index",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new OutputProfile("pages", "tests")
        );
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/demo.md",
                List.of(new NormalizedRequirement(
                        "REQ-001",
                        "User can inspect an account profile summary",
                        "The user can open the account profile page.",
                        "Account profile summary is visible.",
                        true,
                        false,
                        List.of("functional-requirements"),
                        new SourceReference("requirements/demo.md", 1, 1, "")
                )),
                List.of(),
                List.of()
        );

        CanonicalTestCaseBundle bundle = new ScenarioPipelineRequirementToTestCaseGenerator().generate(
                new RequirementToTestCaseInput(profile, requirements, MappedUiKnowledge.empty(), null)
        );

        Assert.assertEquals(bundle.testCases().size(), 1);
        var testCase = bundle.testCases().get(0);
        Assert.assertEquals(testCase.pageName(), "");
        Assert.assertEquals(testCase.route(), "");
        Assert.assertFalse(testCase.targetPages().contains("DetailPage"));
        Assert.assertFalse(testCase.targetPages().contains("RecordDetailsPage"));
    }

    @Test
    public void resolvesSearchToConfirmedRecordListInsteadOfLoginPage() {
        ProjectProfile profile = new ProjectProfile(
                "demo", "Demo App", "https://example.test", "/", "/auth/login", "", "/dashboard/index",
                "", "", "", "", "", "", "", new OutputProfile("pages", "tests")
        );
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/vacancies.md",
                List.of(new NormalizedRequirement(
                        "REQ-001", "User can click the Search button", "User can click the Search button.", "",
                        true, false, List.of("functional-requirements"),
                        new SourceReference("requirements/vacancies.md", 1, 1, "")
                )), List.of(), List.of()
        );
        MappedUiKnowledge knowledge = new MappedUiKnowledge(List.of(new ua.demo.agentlab.ui.discovery.mapping.model.MappedPage(
                "recruitment", "RecruitmentPage", "listing",
                "https://example.test/recruitment/vacancies", "/recruitment/vacancies", "Vacancies",
                List.of(), List.of(), List.of(), List.of(), List.of(), null, "", ""
        )), List.of(), List.of(), List.of(), List.of());

        CanonicalTestCaseBundle bundle = new ScenarioPipelineRequirementToTestCaseGenerator().generate(
                new RequirementToTestCaseInput(profile, requirements, knowledge, null)
        );

        var testCase = bundle.testCases().get(0);
        Assert.assertEquals(testCase.pageName(), "RecruitmentPage");
        Assert.assertEquals(testCase.route(), "/recruitment/vacancies");
        Assert.assertFalse(testCase.sourcePageName().equals("LoginPage"));
        Assert.assertTrue(testCase.operationIntents().stream()
                .anyMatch(intent -> intent.kind().name().equals("SEARCH")));
    }

    @Test
    public void keepsProtectedModuleNavigationInReviewUntilRecordListIsConfirmed() {
        ProjectProfile profile = new ProjectProfile(
                "demo", "Demo App", "https://example.test", "/", "/auth/login", "", "/dashboard/index",
                "", "", "", "", "", "", "", new OutputProfile("pages", "tests")
        );
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/recruitment.md",
                List.of(new NormalizedRequirement(
                        "REQ-001", "User can open the Recruitment module",
                        "User can open the Recruitment module from the authenticated application area.", "",
                        true, false, List.of("functional-requirements"),
                        new SourceReference("requirements/recruitment.md", 1, 1, "")
                )), List.of(), List.of()
        );

        CanonicalTestCaseBundle bundle = new ScenarioPipelineRequirementToTestCaseGenerator().generate(
                new RequirementToTestCaseInput(profile, requirements, MappedUiKnowledge.empty(), null)
        );

        var testCase = bundle.testCases().get(0);
        Assert.assertEquals(testCase.pageName(), "");
        Assert.assertEquals(testCase.route(), "");
        Assert.assertEquals(testCase.sourcePageName(), "DashboardPage");
    }
}
