package ua.demo.agentlab.ai.expectationenrichment.agent;

import ua.demo.agentlab.ai.expectationenrichment.model.ExpectedResultCandidate;
import ua.demo.agentlab.ai.expectationenrichment.model.ResolvedExpectedResult;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.util.List;

public record TestCaseExpectationEnrichmentOutput(
        CanonicalTestCaseBundle bundle,
        List<ExpectedResultCandidate> candidates,
        List<ResolvedExpectedResult> resolved,
        List<String> failures
) {
    public TestCaseExpectationEnrichmentOutput {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        resolved = resolved == null ? List.of() : List.copyOf(resolved);
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
}
