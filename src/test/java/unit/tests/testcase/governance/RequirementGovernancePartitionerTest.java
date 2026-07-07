package unit.tests.testcase.governance;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.testcase.governance.RequirementGovernanceBundle;
import ua.demo.agentlab.testcase.governance.RequirementGovernanceCategory;
import ua.demo.agentlab.testcase.governance.RequirementGovernancePartitioner;

import java.util.List;

public class RequirementGovernancePartitionerTest {

    @Test
    public void separatesExecutableRequirementsFromGovernanceRules() {
        NormalizedRequirementBundle bundle = new NormalizedRequirementBundle(
                "requirements/valid-login-requirement.md",
                List.of(
                        requirement("REQ-001", "Valid login", "User can authenticate with valid credentials",
                                "functional-requirements"),
                        requirement("REQ-002", "Login route", "Login route contains configured project login route",
                                "ui-expectations"),
                        requirement("REQ-003", "Credentials", "Username and password must come from configured test data",
                                "ui-expectations"),
                        requirement("REQ-004", "No weak locators", "Locator score must pass quality gate",
                                "quality-expectations"),
                        requirement("REQ-005", "Runtime evidence", "Network evidence should confirm auth transition",
                                "runtime-evidence-expectations"),
                        requirement("REQ-006", "External links", "External footer navigation is out of scope",
                                "out-of-scope"),
                        requirement("REQ-007", "Locator stability", "Generated locators must be stable across runs",
                                "non-functional-requirements")
                ),
                List.of(),
                List.of()
        );

        RequirementGovernanceBundle result = new RequirementGovernancePartitioner().partition(bundle);

        Assert.assertEquals(result.totalRequirements(), 7);
        Assert.assertEquals(result.canonicalTestCaseRequirements(), 2);
        Assert.assertEquals(result.testCaseRequirements().size(), 2);
        Assert.assertEquals(result.governanceRequirements().size(), 5);
        Assert.assertTrue(result.testCaseRequirements().stream()
                .allMatch(item -> item.testCaseEligible()));
        Assert.assertTrue(result.governanceRequirements().stream()
                .noneMatch(item -> item.testCaseEligible()));
        Assert.assertTrue(result.governanceRequirements().stream()
                .anyMatch(item -> item.category() == RequirementGovernanceCategory.TEST_DATA_RULE));
        Assert.assertTrue(result.governanceRequirements().stream()
                .anyMatch(item -> item.category() == RequirementGovernanceCategory.QUALITY_RULE));
        Assert.assertTrue(result.governanceRequirements().stream()
                .anyMatch(item -> item.category() == RequirementGovernanceCategory.RUNTIME_EVIDENCE_RULE));
        Assert.assertTrue(result.governanceRequirements().stream()
                .anyMatch(item -> item.category() == RequirementGovernanceCategory.OUT_OF_SCOPE));
        Assert.assertTrue(result.governanceRequirements().stream()
                .anyMatch(item -> item.category() == RequirementGovernanceCategory.NON_FUNCTIONAL_RULE));
    }

    private NormalizedRequirement requirement(String id, String title, String statement, String tag) {
        return new NormalizedRequirement(
                id,
                title,
                statement,
                "",
                true,
                false,
                List.of(tag),
                new SourceReference("requirements/valid-login-requirement.md", 1, 1, title)
        );
    }
}
