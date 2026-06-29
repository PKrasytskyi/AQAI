package ua.demo.agentlab.ui.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.generator.UiTestPlanGenerator;

import java.util.Set;

public class UiTestPlanAgent implements WorkflowAgent {

    private final UiTestPlanGenerator uiTestPlanGenerator;

    public UiTestPlanAgent(UiTestPlanGenerator uiTestPlanGenerator){
        this.uiTestPlanGenerator = uiTestPlanGenerator;
    }

    @Override
    public String name() {
        return "ui-test-plan-agent";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.UI_TEST_PLAN);
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getCanonicalTestCaseBundle() != null && state.getUiTestPlan() == null;
    }

    @Override
    public void execute(WorkflowState state) {
        UiTestPlan uiTestPlan = uiTestPlanGenerator.generate(state);
        state.setUiTestPlan(uiTestPlan);
        state.addArtifact("ui.test.plan.primary.page", uiTestPlan.targetPage());
        state.addArtifact("ui.test.plan.pages", String.join(", ", uiTestPlan.pageNames()));
        state.addArtifact("ui.test.plan.scenario.count", String.valueOf(uiTestPlan.scenarios().size()));
        state.addFinding("UI test plan created with " + uiTestPlan.scenarios().size() + " UI scenarios");
    }
}
