package ua.demo.agentlab.ui.testcontract.model;

import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;

import java.util.List;

public record UiTestContractBundle(
        String schemaVersion,
        List<UiTestContractSpec> contracts
) {
    public UiTestContractBundle {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? LlmOutputSchemaVersion.UI_TEST_CONTRACT_BUNDLE
                : schemaVersion.trim();
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
    }
}
