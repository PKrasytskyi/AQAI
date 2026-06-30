package ua.demo.agentlab.validation;

import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.List;

public interface GeneratedCodeValidator {

    GeneratedCodeValidationResult validate(List<String> writtenFiles);

    default GeneratedCodeValidationResult validate(WorkflowState state) {
        return validate(state == null ? List.of() : state.getWrittenFiles());
    }
}
