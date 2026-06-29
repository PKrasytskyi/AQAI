package ua.demo.agentlab.validation.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.validation.GeneratedUiContractValidationResult;
import ua.demo.agentlab.validation.GeneratedUiContractValidator;

public class GeneratedUiContractValidationAgent implements WorkflowAgent {

    private final GeneratedUiContractValidator validator;

    public GeneratedUiContractValidationAgent(GeneratedUiContractValidator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("validator cannot be null");
        }
        this.validator = validator;
    }

    @Override
    public String name() {
        return "generated-ui-contract-validation-agent";
    }

    @Override
    public int order() {
        return 65;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return !state.getPageObjectFiles().isEmpty() && !state.getUiTestFiles().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        GeneratedUiContractValidationResult result = validator.validate(state);
        state.setGeneratedUiContractValidationResult(result);
        state.addArtifact("generated.ui.contract.validation.status", result.status().name());
        state.addArtifact("generated.ui.contract.validation.summary", result.summary());
        state.addFinding(result.summary());

        if (result.isFailed()) {
            state.fail("Generated UI contract validation failed:\n- " + String.join("\n- ", result.violations()));
        }
    }
}
