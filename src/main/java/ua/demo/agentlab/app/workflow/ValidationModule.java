package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.validation.GeneratedCodeValidator;

public record ValidationModule(
        GeneratedCodeValidator generatedCodeValidator,
        WorkflowAgent generatedUiContractValidationAgent,
        WorkflowAgent generatedCodeCompileAgent,
        WorkflowAgent generatedCodeReviewAgent
) {
}
