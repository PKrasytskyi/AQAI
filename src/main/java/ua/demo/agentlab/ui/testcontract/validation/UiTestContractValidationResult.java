package ua.demo.agentlab.ui.testcontract.validation;

import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidationReport;

public record UiTestContractValidationResult(
        LlmOutputSchemaValidationReport schemaReport,
        UiTestContractQualityReport qualityReport
) {
    public boolean valid() {
        return schemaReport != null
                && schemaReport.valid()
                && qualityReport != null
                && !qualityReport.hasBlockingIssues();
    }
}
