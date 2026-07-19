package ua.demo.agentlab.ui.testcontract.validation;

import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;

import java.util.List;

public record UiTestContractValidationInput(
        UiTestContractBundle bundle,
        CanonicalTestCaseBundle canonicalTestCases,
        List<PomContractSpec> pomContracts
) {
    public UiTestContractValidationInput {
        pomContracts = pomContracts == null ? List.of() : List.copyOf(pomContracts);
    }
}
