package ua.demo.agentlab.artifactreuse.planner;

import ua.demo.agentlab.artifactreuse.flow.FlowContractBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

public record ReusePlannerInput(
        CanonicalTestCaseBundle canonicalTestCases,
        FlowContractBundle currentRunFlows,
        FlowSemanticCandidateBundle semanticCandidates,
        boolean enabled,
        boolean explainDecisions
) {
}
