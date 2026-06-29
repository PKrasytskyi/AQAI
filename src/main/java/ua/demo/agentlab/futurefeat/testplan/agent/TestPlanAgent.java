package ua.demo.agentlab.futurefeat.testplan.agent;

import ua.demo.agentlab.futurefeat.testplan.generator.TestPlanGenerator;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;

public class TestPlanAgent implements WorkflowAgent {

    private final TestPlanGenerator testPlanGenerator;

    public TestPlanAgent(TestPlanGenerator testPlanGenerator) {
        this.testPlanGenerator = testPlanGenerator;
    }

    @Override
    public String name() {
        return "test-plan-agent";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getNormalizedRequirementBundle() != null && state.getTestPlan() == null;
    }

    @Override
    public void execute(WorkflowState state) {
        TestPlan testPlan = testPlanGenerator.generate(state.getNormalizedRequirementBundle(), state);

        state.setTestPlan(testPlan);
        state.addArtifact("test.plan.source", testPlan.source());
        state.addArtifact(
                "test.plan.summary",
                "areas=%d, scenarios=%d, assumptions=%d, risks=%d".formatted(
                        testPlan.functionalAreas().size(),
                        testPlan.scenarios().size(),
                        testPlan.assumptions().size(),
                        testPlan.risks().size()
                )
        );
        state.addFinding("Test plan created with " + testPlan.scenarios().size() + " scenarios");
    }
}
