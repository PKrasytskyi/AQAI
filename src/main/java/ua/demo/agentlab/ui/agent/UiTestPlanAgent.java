package ua.demo.agentlab.ui.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.generator.UiTestPlanGenerator;

import java.util.Set;

public class UiTestPlanAgent implements WorkflowAgent,
        PipelineAgent<CanonicalTestCaseBundle, UiTestPlan> {

    private final UiTestPlanGenerator uiTestPlanGenerator;

    public UiTestPlanAgent(UiTestPlanGenerator uiTestPlanGenerator){
        this.uiTestPlanGenerator = uiTestPlanGenerator;
    }

    @Override
    public String name() {
        return "ui-test-plan-agent";
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
    public WorkflowArtifact input() {
        return WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_TEST_PLAN;
    }

    @Override
    public UiTestPlan execute(CanonicalTestCaseBundle input, WorkflowRunEnvelope run) {
        return uiTestPlanGenerator.generate(input);
    }

    @Override
    public void applyOutput(UiTestPlan uiTestPlan, WorkflowState state) {
        state.setUiTestPlan(uiTestPlan);
        state.addArtifact("ui.test.plan.primary.page", uiTestPlan.targetPage());
        state.addArtifact("ui.test.plan.pages", String.join(", ", uiTestPlan.pageNames()));
        state.addArtifact("ui.test.plan.scenario.count", String.valueOf(uiTestPlan.scenarios().size()));
        state.addFinding("UI test plan created with " + uiTestPlan.scenarios().size() + " UI scenarios");
    }
}
