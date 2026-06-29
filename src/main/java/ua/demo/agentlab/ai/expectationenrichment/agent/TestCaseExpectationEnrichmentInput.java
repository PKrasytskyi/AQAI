package ua.demo.agentlab.ai.expectationenrichment.agent;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

public record TestCaseExpectationEnrichmentInput(
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        NormalizedRequirementBundle normalizedRequirementBundle,
        ProjectProfile projectProfile
) {
}
