package ua.demo.agentlab.futurefeat.testplan.generator;

import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

public interface TestPlanGenerator {

    TestPlan generate(NormalizedRequirementBundle bundle, WorkflowState state);
}
