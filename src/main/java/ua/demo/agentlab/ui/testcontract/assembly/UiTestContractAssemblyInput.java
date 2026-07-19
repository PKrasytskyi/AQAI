package ua.demo.agentlab.ui.testcontract.assembly;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.util.List;

public record UiTestContractAssemblyInput(
        CanonicalTestCaseBundle canonicalTestCases,
        List<AssertionContract> assertionContracts,
        List<PomContractSpec> pomContracts
) {
    public UiTestContractAssemblyInput {
        assertionContracts = assertionContracts == null ? List.of() : List.copyOf(assertionContracts);
        pomContracts = pomContracts == null ? List.of() : List.copyOf(pomContracts);
    }
}
