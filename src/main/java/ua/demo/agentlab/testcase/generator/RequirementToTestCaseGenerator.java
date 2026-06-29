package ua.demo.agentlab.testcase.generator;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

public interface RequirementToTestCaseGenerator {

    CanonicalTestCaseBundle generate(WorkflowState state);

    default CanonicalTestCaseBundle generate(RequirementToTestCaseInput input) {
        throw new UnsupportedOperationException("Typed RequirementToTestCaseInput is not supported by this generator");
    }
}
