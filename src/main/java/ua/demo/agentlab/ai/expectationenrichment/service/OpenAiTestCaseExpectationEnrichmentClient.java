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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAiTestCaseExpectationEnrichmentClient implements TestCaseExpectationEnrichmentClient {

    private final TestCaseExpectationEnrichmentClient fallbackClient;
    private final OpenAiResponseGenerationClient generationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmOutputSchemaValidator schemaValidator = new LlmOutputSchemaValidator();
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
            if ("project-profile-route".equals(defaultResult.source())) {
                results.add(defaultResult);
                continue;
            }
            try {
                results.add(parse(
                        generationClient.generate(prompt(testCase, candidates, projectProfile)),
                        testCase,
                        candidates,
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
                4. expectedValue must exactly equal the selected candidate's expectedResult, or the test case's supplied route assertion value.
                5. Use status=resolved only when confidence is at least 0.80; otherwise use needs-review.

                # Input
                %s

                # Expected Output
                Schema version: %s.
                {"schemaVersion":"%s","testCaseId":"%s","candidateRequirementId":"","expectedValue":"","confidence":0.0,"status":"needs-review","rationale":""}

                # Success Criteria
                The expected result is traceable to an Assertion Requirement or a project-profile route.

                # Notes
                This is enrichment metadata. Do not generate Java, Page Objects, or tests.
                """.formatted(objectMapper.writeValueAsString(Map.of(
                "testCase", testCase,
                "assertionResultCandidates", candidates == null ? List.of() : candidates,
                "projectRoutes", profile == null ? Map.of() : Map.of(
                        "home", profile.homeRoute(),
                        "login", profile.loginRoute(),
                        "authenticated", profile.authenticatedRoute()
                )
        )),
                LlmOutputSchemaVersion.RESOLVED_EXPECTED_RESULT,
                LlmOutputSchemaVersion.RESOLVED_EXPECTED_RESULT,
                testCase.id());
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
