package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.testcase.agent.RequirementToTestCaseAgent;
import ua.demo.agentlab.testcase.planning.ScenarioPipelineRequirementToTestCaseGenerator;
import ua.demo.agentlab.ui.agent.UiTestPlanAgent;
import ua.demo.agentlab.ui.generator.CanonicalTestCaseUiPlanGenerator;

public class TestPlanningModuleFactory {

    public TestPlanningModule create() {
        return new TestPlanningModule(
                new RequirementToTestCaseAgent(new ScenarioPipelineRequirementToTestCaseGenerator()),
                new UiTestPlanAgent(new CanonicalTestCaseUiPlanGenerator())
        );
    }
}
