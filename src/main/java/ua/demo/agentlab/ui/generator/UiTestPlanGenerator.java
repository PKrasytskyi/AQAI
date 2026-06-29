package ua.demo.agentlab.ui.generator;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.UiTestPlan;

public interface UiTestPlanGenerator {

    UiTestPlan generate(WorkflowState state);
}
