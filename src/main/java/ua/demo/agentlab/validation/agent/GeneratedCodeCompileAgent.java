package ua.demo.agentlab.validation.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.GeneratedCodeValidator;

public class GeneratedCodeCompileAgent implements WorkflowAgent {

    private final GeneratedCodeValidator validator;

    public GeneratedCodeCompileAgent(GeneratedCodeValidator validator) {
        this.validator = validator;
    }

    @Override
    public String name() {
        return "generated-code-compile-agent";
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return !state.getWrittenFiles().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        GeneratedCodeValidationResult result = validator.validate(state);

        state.addArtifact("generated.code.validation.status", result.status().name());
        state.addArtifact("generated.code.validation.summary", result.summary());
        state.addFinding(result.summary());
        state.setGeneratedCodeValidationResult(result);

        if (result.isFailed()) {
            state.fail("Generated code validation failed:\n" + result.compilerOutput());
        }
    }
}
