package ua.demo.agentlab.validation;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.persistence.GeneratedUiSources;

public interface GeneratedUiContractValidator {

    GeneratedUiContractValidationResult validate(GeneratedUiSources sources);

    default GeneratedUiContractValidationResult validate(WorkflowState state) {
        return validate(state == null
                ? null
                : new GeneratedUiSources(state.getPageObjectFiles(), state.getUiTestFiles()));
    }
}
