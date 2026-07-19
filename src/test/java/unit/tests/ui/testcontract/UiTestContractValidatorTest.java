package unit.tests.ui.testcontract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.testcontract.assembly.UiTestContractAssembler;
import ua.demo.agentlab.ui.testcontract.assembly.UiTestContractAssemblyInput;
import ua.demo.agentlab.ui.testcontract.model.UiTestActionSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestArgumentSource;
import ua.demo.agentlab.ui.testcontract.model.UiTestArgumentSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractSpec;
import ua.demo.agentlab.ui.testcontract.validation.UiTestContractValidationInput;
import ua.demo.agentlab.ui.testcontract.validation.UiTestContractValidator;

import java.util.ArrayList;
import java.util.List;

public class UiTestContractValidatorTest {

    private final UiTestContractValidator validator = new UiTestContractValidator();

    @Test
    public void acceptsSchemaValidOwnedAtomicContracts() {
        var result = validate(bundle());

        Assert.assertTrue(result.schemaReport().valid(), result.schemaReport().issues().toString());
        Assert.assertFalse(result.qualityReport().hasBlockingIssues(), result.qualityReport().issues().toString());
        Assert.assertEquals(result.qualityReport().readyContracts(), 4);
    }

    @Test
    public void blocksUndeclaredPomMethod() {
        UiTestContractSpec original = bundle().contracts().get(1);
        List<UiTestActionSpec> actions = new ArrayList<>(original.actions());
        UiTestActionSpec first = actions.get(0);
        actions.set(0, new UiTestActionSpec(
                first.order(), first.page(), "rawDriverLogin", first.arguments(), first.sourceOperations()
        ));
        UiTestContractSpec invalid = copy(original, actions);
        List<UiTestContractSpec> contracts = new ArrayList<>(bundle().contracts());
        contracts.set(1, invalid);
        UiTestContractBundle changed = new UiTestContractBundle(null, contracts);

        var result = validate(changed);

        Assert.assertTrue(result.qualityReport().hasBlockingIssues());
        Assert.assertTrue(result.qualityReport().issues().stream()
                .anyMatch(issue -> issue.ruleId().equals("TEST_CONTRACT_ACTION_DECLARED")));
    }

    @Test
    public void blocksLiteralCredentialValues() {
        UiTestContractSpec original = bundle().contracts().get(1);
        List<UiTestActionSpec> actions = new ArrayList<>(original.actions());
        UiTestActionSpec first = actions.get(0);
        actions.set(0, new UiTestActionSpec(
                first.order(),
                first.page(),
                first.method(),
                List.of(new UiTestArgumentSpec("username", UiTestArgumentSource.LITERAL, "", "", "Admin")),
                first.sourceOperations()
        ));
        List<UiTestContractSpec> contracts = new ArrayList<>(bundle().contracts());
        contracts.set(1, copy(original, actions));
        UiTestContractBundle changed = new UiTestContractBundle(null, contracts);

        var result = validate(changed);

        Assert.assertTrue(result.qualityReport().issues().stream()
                .anyMatch(issue -> issue.ruleId().equals("TEST_CONTRACT_NO_LITERAL_SECRETS")));
    }

    private UiTestContractBundle bundle() {
        return new UiTestContractAssembler().assemble(new UiTestContractAssemblyInput(
                UiTestContractFixtures.canonicalBundle(),
                UiTestContractFixtures.assertionContracts(),
                UiTestContractFixtures.pomContracts()
        ));
    }

    private ua.demo.agentlab.ui.testcontract.validation.UiTestContractValidationResult validate(
            UiTestContractBundle bundle
    ) {
        return validator.validate(new UiTestContractValidationInput(
                bundle,
                UiTestContractFixtures.canonicalBundle(),
                UiTestContractFixtures.pomContracts()
        ));
    }

    private UiTestContractSpec copy(UiTestContractSpec original, List<UiTestActionSpec> actions) {
        return new UiTestContractSpec(
                original.scenarioId(),
                original.capability(),
                original.className(),
                original.testMethodName(),
                original.description(),
                original.sourcePage(),
                original.targetPage(),
                original.preconditions(),
                actions,
                original.assertions(),
                original.dataReferences(),
                original.requirementIds(),
                original.coverageGaps()
        );
    }
}
