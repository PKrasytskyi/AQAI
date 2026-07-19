package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.ui.contract.PomContractQualityReport;
import ua.demo.agentlab.ai.ui.contract.PomContractScopeValidator;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

/** Runs the terminal POM scope invariant without writing artifacts. */
public final class PomContractValidationStage {
    private final PomContractScopeValidator validator;

    public PomContractValidationStage(PomContractScopeValidator validator) {
        this.validator = validator;
    }

    public PomContractQualityReport validate(PomContractSpec contract, PromptReadyPomScope scope) {
        return validator.validate(contract, scope);
    }
}
