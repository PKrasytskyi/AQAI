package ua.demo.agentlab.ai.expectationenrichment.service;

import ua.demo.agentlab.ai.expectationenrichment.model.ExpectedResultCandidate;
import ua.demo.agentlab.ai.expectationenrichment.model.ResolvedExpectedResult;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class RuleBasedTestCaseExpectationEnrichmentClient implements TestCaseExpectationEnrichmentClient {

    @Override
    public List<ResolvedExpectedResult> resolve(
            List<CanonicalTestCase> testCases,
            List<ExpectedResultCandidate> candidates,
            ProjectProfile projectProfile
    ) {
        if (testCases == null || testCases.isEmpty()) {
            return List.of();
        }
        List<ExpectedResultCandidate> safeCandidates = candidates == null ? List.of() : candidates;
        return testCases.stream().map(testCase -> resolveOne(testCase, safeCandidates)).toList();
    }

    private ResolvedExpectedResult resolveOne(
            CanonicalTestCase testCase,
            List<ExpectedResultCandidate> candidates
    ) {
        String routeExpectation = testCase.assertionIntents().stream()
                .filter(intent -> intent.kind() == AssertionIntentKind.URL_CONTAINS)
                .map(AssertionIntent::expectedValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
        if (!routeExpectation.isBlank()) {
            return new ResolvedExpectedResult(
                    testCase.id(), routeExpectation, testCase.id(), "project-profile-route", 1.0d,
                    "resolved", "Exact route expectation is resolved deterministically from the project profile."
            );
        }

        ExpectedResultCandidate ownCandidate = candidates.stream()
                .filter(candidate -> testCase.requirementRefs().contains(candidate.requirementId()))
                .findFirst()
                .orElse(null);
        if (ownCandidate != null) {
            return resolved(testCase, ownCandidate, 0.95d, "The test case is itself an assertion requirement.");
        }

        ExpectedResultCandidate bestCandidate = candidates.stream()
                .max(Comparator.comparingDouble(candidate -> similarity(testCase, candidate)))
                .orElse(null);
        if (bestCandidate != null && similarity(testCase, bestCandidate) >= 0.80d) {
            return resolved(testCase, bestCandidate, similarity(testCase, bestCandidate),
                    "Rule-based semantic match against Assertion Requirements.");
        }

        String fallback = testCase.assertionIntents().stream()
                .map(AssertionIntent::expectedValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
        return new ResolvedExpectedResult(
                testCase.id(), fallback, testCase.id(), "requirement-fallback", 0.70d,
                "needs-review", "No assertion requirement matched deterministically."
        );
    }

    private ResolvedExpectedResult resolved(
            CanonicalTestCase testCase,
            ExpectedResultCandidate candidate,
            double confidence,
            String rationale
    ) {
        return new ResolvedExpectedResult(
                testCase.id(), candidate.expectedResult(), candidate.requirementId(), "assertion-requirement",
                confidence, "resolved", rationale
        );
    }

    private double similarity(CanonicalTestCase testCase, ExpectedResultCandidate candidate) {
        Set<String> testTokens = tokens(testCase.title() + " " + String.join(" ", testCase.actions())
                + " " + String.join(" ", testCase.assertions()));
        Set<String> candidateTokens = tokens(candidate.expectedResult());
        if (testTokens.isEmpty() || candidateTokens.isEmpty()) {
            return 0.0d;
        }
        long overlap = candidateTokens.stream().filter(testTokens::contains).count();
        return (double) overlap / (double) candidateTokens.size();
    }

    private Set<String> tokens(String value) {
        return java.util.Arrays.stream((value == null ? "" : value).toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", " ").split("\\s+"))
                .filter(token -> token.length() >= 4)
                .collect(Collectors.toSet());
    }
}
