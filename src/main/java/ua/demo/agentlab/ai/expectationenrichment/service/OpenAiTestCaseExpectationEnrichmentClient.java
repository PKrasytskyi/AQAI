package ua.demo.agentlab.ai.expectationenrichment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.expectationenrichment.model.ExpectedResultCandidate;
import ua.demo.agentlab.ai.expectationenrichment.model.ResolvedExpectedResult;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidator;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenAiTestCaseExpectationEnrichmentClient implements TestCaseExpectationEnrichmentClient {

    private final TestCaseExpectationEnrichmentClient fallbackClient;
    private final OpenAiResponseGenerationClient generationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmOutputSchemaValidator schemaValidator = new LlmOutputSchemaValidator();
    private final ExpectedResultConflictDetector conflictDetector = new ExpectedResultConflictDetector();
    private List<String> lastFailures = List.of();

    public OpenAiTestCaseExpectationEnrichmentClient(RagRuntimeConfig config) {
        this(config, new RuleBasedTestCaseExpectationEnrichmentClient());
    }

    OpenAiTestCaseExpectationEnrichmentClient(
            RagRuntimeConfig config,
            TestCaseExpectationEnrichmentClient fallbackClient
    ) {
        this.generationClient = new OpenAiResponseGenerationClient(config);
        this.fallbackClient = fallbackClient == null ? new RuleBasedTestCaseExpectationEnrichmentClient() : fallbackClient;
    }

    @Override
    public List<ResolvedExpectedResult> resolve(
            List<CanonicalTestCase> testCases,
            List<ExpectedResultCandidate> candidates,
            ProjectProfile projectProfile
    ) {
        List<ResolvedExpectedResult> fallback = fallbackClient.resolve(testCases, candidates, projectProfile);
        List<ResolvedExpectedResult> results = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (int index = 0; index < fallback.size(); index++) {
            CanonicalTestCase testCase = testCases.get(index);
            ResolvedExpectedResult defaultResult = fallback.get(index);
            if (defaultResult.isApproved() || "conflict-detector".equals(defaultResult.source())) {
                results.add(defaultResult);
                continue;
            }
            List<ExpectedResultCandidate> scopedCandidates = scopedCandidates(testCase, candidates, defaultResult);
            if (scopedCandidates.isEmpty()) {
                results.add(defaultResult);
                continue;
            }
            try {
                results.add(parse(
                        generationClient.generate(prompt(testCase, scopedCandidates, projectProfile)),
                        testCase,
                        scopedCandidates,
                        defaultResult
                ));
            } catch (Exception exception) {
                results.add(defaultResult);
                failures.add(testCase.id() + ": " + message(exception));
            }
        }
        lastFailures = List.copyOf(failures);
        return List.copyOf(results);
    }

    public List<String> lastFailures() {
        return lastFailures;
    }

    private String prompt(
            CanonicalTestCase testCase,
            List<ExpectedResultCandidate> candidates,
            ProjectProfile profile
    ) throws Exception {
        return """
                # Goal
                Resolve one concrete expected result for one UI test case.

                # Context
                Assertion Requirements are the only authority for expected business outcomes. Project profile routes are authoritative for route expectations.

                # Constraints
                1. Return JSON only.
                2. Select at most one supplied assertion-result candidate.
                3. Do not invent expected text, routes, locators, methods, or requirements.
                4. expectedValue must exactly equal the selected candidate's expectedResult, or the test case's routeAssertionValue.
                5. Use status=resolved only when confidence is at least 0.80 and conflictSignals is empty; otherwise use needs-review.

                # Input
                %s

                # Expected Output
                Schema version: %s.
                {"schemaVersion":"%s","testCaseId":"%s","candidateRequirementId":"","expectedValue":"","confidence":0.0,"status":"needs-review","rationale":""}

                # Success Criteria
                The expected result is traceable to an Assertion Requirement or a project-profile route.

                # Notes
                This is enrichment metadata. Do not generate Java, Page Objects, or tests.
                """.formatted(objectMapper.writeValueAsString(promptInput(testCase, candidates, profile)),
                LlmOutputSchemaVersion.RESOLVED_EXPECTED_RESULT,
                LlmOutputSchemaVersion.RESOLVED_EXPECTED_RESULT,
                testCase.id());
    }

    private Map<String, Object> promptInput(
            CanonicalTestCase testCase,
            List<ExpectedResultCandidate> candidates,
            ProjectProfile profile
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("assertionResultCandidates", candidates == null ? List.of() : candidates);
        input.put("projectRoutes", projectRoutes(profile));
        input.put("testCase", compactTestCase(testCase));
        input.put("conflictSignals", conflictDetector.detect(testCase));
        return input;
    }

    private Map<String, Object> compactTestCase(CanonicalTestCase testCase) {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("id", testCase.id());
        compact.put("title", testCase.title());
        compact.put("requirementRefs", testCase.requirementRefs());
        compact.put("sourcePageName", testCase.sourcePageName());
        compact.put("pageName", testCase.pageName());
        compact.put("sourceRoute", testCase.sourceRoute());
        compact.put("route", testCase.route());
        compact.put("operationKinds", testCase.operationIntents().stream()
                .map(intent -> intent.kind().name())
                .distinct()
                .toList());
        compact.put("assertionKinds", testCase.assertionIntents().stream()
                .map(intent -> intent.kind().name())
                .distinct()
                .toList());
        compact.put("routeAssertionValue", routeAssertionValue(testCase));
        compact.put("actions", testCase.actions().stream().limit(5).toList());
        compact.put("assertions", testCase.assertions().stream().limit(5).toList());
        return compact;
    }

    private List<ExpectedResultCandidate> scopedCandidates(
            CanonicalTestCase testCase,
            List<ExpectedResultCandidate> candidates,
            ResolvedExpectedResult fallback
    ) {
        List<ExpectedResultCandidate> safeCandidates = candidates == null ? List.of() : candidates;
        List<ExpectedResultCandidate> exact = safeCandidates.stream()
                .filter(candidate -> testCase.requirementRefs().contains(candidate.requirementId())
                        || candidate.requirementId().equals(testCase.id())
                        || candidate.requirementId().equals(fallback.sourceRequirementId()))
                .filter(candidate -> conflictDetector.detect(testCase, candidate).isEmpty())
                .distinct()
                .toList();
        if (!exact.isEmpty()) {
            return exact.stream().limit(3).toList();
        }
        return safeCandidates.stream()
                .filter(candidate -> conflictDetector.detect(testCase, candidate).isEmpty())
                .map(candidate -> Map.entry(candidate, similarity(testCase, candidate)))
                .filter(entry -> entry.getValue() >= 0.35d)
                .sorted(Map.Entry.<ExpectedResultCandidate, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String routeAssertionValue(CanonicalTestCase testCase) {
        return testCase.assertionIntents().stream()
                .filter(intent -> intent.kind() == AssertionIntentKind.URL_CONTAINS)
                .map(intent -> intent.expectedValue() == null ? "" : intent.expectedValue())
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private Map<String, String> projectRoutes(ProjectProfile profile) {
        if (profile == null) {
            return Map.of();
        }
        Map<String, String> routes = new LinkedHashMap<>();
        if (!profile.homeRoute().isBlank()) {
            routes.put("home", profile.homeRoute());
        }
        if (!profile.loginRoute().isBlank()) {
            routes.put("login", profile.loginRoute());
        }
        if (!profile.authenticatedRoute().isBlank()) {
            routes.put("authenticated", profile.authenticatedRoute());
        }
        return routes;
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
        return java.util.Arrays.stream((value == null ? "" : value)
                        .toLowerCase(java.util.Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", " ")
                        .split("\\s+"))
                .filter(token -> token.length() >= 4)
                .collect(Collectors.toSet());
    }

    private ResolvedExpectedResult parse(
            String response,
            CanonicalTestCase testCase,
            List<ExpectedResultCandidate> candidates,
            ResolvedExpectedResult fallback
    ) throws Exception {
        JsonNode node = objectMapper.readTree(stripFences(response));
        schemaValidator.throwIfInvalid(schemaValidator.validateResolvedExpectedResult(node));
        if (!testCase.id().equals(text(node, "testCaseId"))) {
            return fallback;
        }
        String candidateId = text(node, "candidateRequirementId");
        ExpectedResultCandidate candidate = (candidates == null ? List.<ExpectedResultCandidate>of() : candidates).stream()
                .filter(value -> value.requirementId().equals(candidateId))
                .findFirst()
                .orElse(null);
        if (candidate == null) {
            return fallback;
        }
        if (!conflictDetector.detect(testCase, candidate).isEmpty()) {
            return new ResolvedExpectedResult(
                    testCase.id(), "", candidate.requirementId(), "conflict-detector", 0.0d,
                    "needs-review", String.join(" ", conflictDetector.detect(testCase, candidate))
            );
        }
        double confidence = node.path("confidence").isNumber() ? node.path("confidence").asDouble() : 0.0d;
        String status = text(node, "status");
        if (!"resolved".equalsIgnoreCase(status) || confidence < 0.80d) {
            return fallback;
        }
        return new ResolvedExpectedResult(
                testCase.id(),
                candidate.expectedResult(),
                candidate.requirementId(),
                "assertion-requirement",
                confidence,
                "resolved",
                text(node, "rationale")
        );
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }

    private String stripFences(String response) {
        String value = response == null ? "" : response.trim();
        return value.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
    }

    private String message(Exception exception) {
        String value = exception == null ? "" : exception.getMessage();
        return value == null || value.isBlank() ? "unknown failure" : value.trim();
    }
}
