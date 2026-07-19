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

    private final ExpectedResultConflictDetector conflictDetector = new ExpectedResultConflictDetector();

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
        List<String> conflicts = conflictDetector.detect(testCase);
        if (!conflicts.isEmpty()) {
            return new ResolvedExpectedResult(
                    testCase.id(),
                    "",
                    firstRequirementRef(testCase),
                    "conflict-detector",
                    0.0d,
                    "needs-review",
                    String.join(" ", conflicts)
            );
        }

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
                .filter(candidate -> conflictDetector.detect(testCase, candidate).isEmpty())
                .findFirst()
                .orElse(null);
        if (ownCandidate != null) {
            return resolved(testCase, ownCandidate, 0.95d, "The test case is itself an assertion requirement.");
        }

        ExpectedResultCandidate semanticCandidate = semanticCandidate(testCase, candidates);
        if (semanticCandidate != null && conflictDetector.detect(testCase, semanticCandidate).isEmpty()) {
            return resolved(testCase, semanticCandidate, 0.90d,
                    "Rule-based login flow semantic match against Assertion Requirements.");
        }

        ExpectedResultCandidate bestCandidate = candidates.stream()
                .filter(candidate -> conflictDetector.detect(testCase, candidate).isEmpty())
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

    private ExpectedResultCandidate semanticCandidate(
            CanonicalTestCase testCase,
            List<ExpectedResultCandidate> candidates
    ) {
        String text = normalize(testCase.title() + " " + String.join(" ", testCase.actions())
                + " " + String.join(" ", testCase.assertions()));
        if (containsAny(text, "enter a valid password", "password")) {
            return candidateContaining(candidates, "password field is visible");
        }
        if (containsAny(text, "enter a valid username")) {
            return candidateContaining(candidates, "username field is visible");
        }
        if (containsAny(text, "login page displays")) {
            return candidateContaining(candidates, "login button is visible", "password field is visible", "username field is visible");
        }
        if (containsAny(text, "login button")) {
            return candidateContaining(candidates, "login button is visible");
        }
        if (containsAny(text, "open the application home page", "home page", "navigate from the home page")) {
            return candidateContaining(candidates, "login page route contains", "home page is accessible");
        }
        if (containsAny(text, "submit the login form", "valid credentials", "redirected to the authenticated area")) {
            return candidateContaining(candidates, "logged with valid credentials", "authenticated area route contains");
        }
        if (containsAny(text, "successful login state", "authenticated area displays")) {
            return candidateContaining(candidates, "dashboard heading is visible", "authenticated area route contains");
        }
        if (containsAny(text, "logout action", "logout")) {
            return candidateContaining(candidates, "logout action is visible");
        }
        return null;
    }

    private ExpectedResultCandidate candidateContaining(List<ExpectedResultCandidate> candidates, String... fragments) {
        for (String fragment : fragments) {
            ExpectedResultCandidate candidate = candidates.stream()
                    .filter(value -> normalize(value.expectedResult()).contains(fragment))
                    .findFirst()
                    .orElse(null);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
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

    private String firstRequirementRef(CanonicalTestCase testCase) {
        return testCase == null || testCase.requirementRefs().isEmpty() ? "" : testCase.requirementRefs().get(0);
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

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
