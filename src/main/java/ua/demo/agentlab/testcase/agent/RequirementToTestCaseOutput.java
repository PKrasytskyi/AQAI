package ua.demo.agentlab.testcase.agent;

import ua.demo.agentlab.testcase.governance.RequirementGovernanceBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

public record RequirementToTestCaseOutput(
        CanonicalTestCaseBundle canonicalTestCaseBundle,
        RequirementGovernanceBundle governanceBundle
) {
    public RequirementToTestCaseOutput {
        if (canonicalTestCaseBundle == null) {
            throw new IllegalArgumentException("canonicalTestCaseBundle cannot be null");
        }
        if (governanceBundle == null) {
            throw new IllegalArgumentException("governanceBundle cannot be null");
        }
    }
}
