package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

public record RequirementPipelineStage(
        RequirementDocument requirementDocument,
        NormalizedRequirementBundle normalizedRequirementBundle,
        TestPlan testPlan
) {
}
