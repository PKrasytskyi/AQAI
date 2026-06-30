package ua.demo.agentlab.futurefeat.testplan.agent;

import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

public record TestPlanGenerationInput(
        NormalizedRequirementBundle bundle,
        String objective,
        RequirementInput requirementInput
) {
}
