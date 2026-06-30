package ua.demo.agentlab.testcase.generator;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

public interface RequirementToTestCaseGenerator {

    CanonicalTestCaseBundle generate(RequirementToTestCaseInput input);

    default CanonicalTestCaseBundle generate(WorkflowState state) {
        return generate(state == null ? null : new RequirementToTestCaseInput(
                state.getProjectProfile(),
                state.getNormalizedRequirementBundle(),
                state.getMappedUiKnowledge(),
                state.getFlowScopedKnowledgePackage()
        ));
    }
}
