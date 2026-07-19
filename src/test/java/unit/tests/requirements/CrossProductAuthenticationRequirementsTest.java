package unit.tests.requirements;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContractBuilder;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;
import ua.demo.agentlab.requirements.source.FileRequirementSource;

import java.util.List;

public class CrossProductAuthenticationRequirementsTest {

    @DataProvider
    public Object[][] authenticationFixtures() {
        return new Object[][]{
                {"requirements/orangehrm-authentication-user-menu-logout.md", "USER_MENU", "DIRECT_CONTROL"},
                {"requirements/the-internet-authentication-logout.md", "DIRECT_CONTROL", "USER_MENU"}
        };
    }

    @Test(dataProvider = "authenticationFixtures")
    public void fixturesProduceFourStructuredAtomicContracts(
            String requirementFile,
            String requiredLogoutAccessMode,
            String forbiddenLogoutAccessMode
    ) {
        var normalized = new RuleBasedRequirementNormalizer().normalize(
                new FileRequirementSource().load(new RequirementInput(SourceType.FILE, requirementFile))
        );
        List<StructuredBehaviorContract> contracts = new StructuredBehaviorContractBuilder()
                .build(normalized.requirements());

        Assert.assertEquals(contracts.size(), 4, "Each fixture must produce four executable contracts");
        Assert.assertEquals(contracts.stream().map(StructuredBehaviorContract::requirementId).distinct().count(), 4L);
        Assert.assertTrue(contracts.stream().allMatch(StructuredBehaviorContract::executable),
                "Invalid contracts: " + contracts.stream()
                        .filter(contract -> !contract.executable())
                        .map(contract -> contract.requirementId() + "=" + contract.reviewReasons())
                        .toList());

        String targetContexts = contracts.stream()
                .map(StructuredBehaviorContract::targetContext)
                .reduce("", (left, right) -> left + " " + right);
        Assert.assertTrue(targetContexts.contains("logoutAccessMode: " + requiredLogoutAccessMode));
        Assert.assertFalse(targetContexts.contains("logoutAccessMode: " + forbiddenLogoutAccessMode));
    }
}
