package ua.demo.agentlab.ai.expectationenrichment.service;

import ua.demo.agentlab.ai.expectationenrichment.model.ExpectedResultCandidate;
import ua.demo.agentlab.ai.expectationenrichment.model.ResolvedExpectedResult;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;

import java.util.List;

public interface TestCaseExpectationEnrichmentClient {

    List<ResolvedExpectedResult> resolve(
            List<CanonicalTestCase> testCases,
            List<ExpectedResultCandidate> candidates,
            ProjectProfile projectProfile
    );
}
