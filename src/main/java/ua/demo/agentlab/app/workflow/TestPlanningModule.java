package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;

public record TestPlanningModule(
        WorkflowAgent requirementToTestCaseAgent,
        WorkflowAgent uiTestPlanAgent
) {
}
