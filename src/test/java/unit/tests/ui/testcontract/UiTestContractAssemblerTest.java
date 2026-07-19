package unit.tests.ui.testcontract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.testcontract.assembly.UiTestContractAssembler;
import ua.demo.agentlab.ui.testcontract.assembly.UiTestContractAssemblyInput;
import ua.demo.agentlab.ui.testcontract.model.UiTestDataReferenceType;

public class UiTestContractAssemblerTest {

    private final UiTestContractAssembler assembler = new UiTestContractAssembler();

    @Test
    public void createsExactlyOneAtomicContractPerCanonicalScenario() {
        var bundle = assemble();

        Assert.assertEquals(bundle.schemaVersion(), "ui-test-contract-bundle.v1");
        Assert.assertEquals(bundle.contracts().size(), 4);
        Assert.assertEquals(bundle.contracts().stream().map(contract -> contract.scenarioId()).toList(),
                java.util.List.of("REQ-001", "REQ-002", "REQ-003", "REQ-004"));
    }

    @Test
    public void expandsAuthenticationIntoTypedPomMethodsAndCredentialReference() {
        var contract = assemble().contracts().get(1);

        Assert.assertEquals(contract.preconditions().get(0).method(), "openLogin");
        Assert.assertEquals(
                contract.actions().stream().map(action -> action.method()).toList(),
                java.util.List.of("enterUsername", "enterPassword", "clickLoginButton")
        );
        Assert.assertEquals(contract.dataReferences().size(), 1);
        Assert.assertEquals(contract.dataReferences().get(0).type(), UiTestDataReferenceType.CREDENTIALS);
        Assert.assertEquals(contract.actions().get(0).arguments().get(0).field(), "username");
        Assert.assertEquals(contract.actions().get(1).arguments().get(0).field(), "password");
    }

    @Test
    public void usesCompositeLogoutMethodWithoutDuplicatingOpenMenu() {
        var contract = assemble().contracts().get(3);

        Assert.assertEquals(
                contract.actions().stream().map(action -> action.method()).toList(),
                java.util.List.of("logout")
        );
        Assert.assertFalse(contract.actions().stream().anyMatch(action -> action.method().equals("openUserMenu")));
        Assert.assertEquals(contract.assertions().stream().map(assertion -> assertion.method()).toList(),
                java.util.List.of("elementVisibleUsername", "urlContainsAuthLogin"));
    }

    @Test
    public void keepsUserMenuActionAtomicAfterAuthenticationSetup() {
        var contract = assemble().contracts().get(2);

        Assert.assertEquals(contract.preconditions().stream().map(action -> action.method()).toList(),
                java.util.List.of("openLogin", "enterUsername", "enterPassword", "clickLoginButton"));
        Assert.assertEquals(contract.actions().stream().map(action -> action.method()).toList(),
                java.util.List.of("openUserMenu"));
        Assert.assertEquals(contract.assertions().get(0).method(), "elementVisibleLogout");
    }

    @Test
    public void usesScenarioDataForParameterizedNonAuthenticationActions() {
        var contract = assembler.assemble(new UiTestContractAssemblyInput(
                UiTestContractFixtures.searchCanonicalBundle(),
                UiTestContractFixtures.searchAssertions(),
                java.util.List.of(UiTestContractFixtures.searchPomContract())
        )).contracts().get(0);

        Assert.assertEquals(contract.actions().get(0).method(), "search");
        Assert.assertEquals(contract.dataReferences().get(0).type(), UiTestDataReferenceType.SCENARIO_DATA);
        Assert.assertEquals(contract.dataReferences().get(0).key(), "search-default");
        Assert.assertEquals(contract.actions().get(0).arguments().get(0).field(), "query");
    }

    private ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle assemble() {
        return assembler.assemble(new UiTestContractAssemblyInput(
                UiTestContractFixtures.canonicalBundle(),
                UiTestContractFixtures.assertionContracts(),
                UiTestContractFixtures.pomContracts()
        ));
    }
}
