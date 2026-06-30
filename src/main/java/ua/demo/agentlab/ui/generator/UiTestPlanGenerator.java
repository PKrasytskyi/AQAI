package ua.demo.agentlab.ui.generator;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;

public interface UiTestPlanGenerator {

    UiTestPlan generate(CanonicalTestCaseBundle bundle);

    default UiTestPlan generate(WorkflowState state) {
        return generate(state == null ? null : state.getCanonicalTestCaseBundle());
    }
}
