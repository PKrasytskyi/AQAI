package ua.demo.agentlab.ai.expectationenrichment.agent;

import ua.demo.agentlab.ai.expectationenrichment.model.ExpectedResultCandidate;
import ua.demo.agentlab.ai.expectationenrichment.model.ResolvedExpectedResult;
import ua.demo.agentlab.ai.expectationenrichment.service.OpenAiTestCaseExpectationEnrichmentClient;
import ua.demo.agentlab.ai.expectationenrichment.service.TestCaseExpectationEnrichmentClient;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestCaseExpectationEnrichmentAgent implements WorkflowAgent,
        PipelineAgent<TestCaseExpectationEnrichmentInput, TestCaseExpectationEnrichmentOutput> {

    private final TestCaseExpectationEnrichmentClient enrichmentClient;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public TestCaseExpectationEnrichmentAgent(TestCaseExpectationEnrichmentClient enrichmentClient) {
        if (enrichmentClient == null) {
            throw new IllegalArgumentException("enrichmentClient cannot be null");
        }
        this.enrichmentClient = enrichmentClient;
    }

    @Override
    public String name() {
        return "test-case-expectation-enrichment-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.TEST_CASE_EXPECTATION_ENRICHMENT
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.TEST_CASE_EXPECTATION_ENRICHMENT;
    }

    @Override
    public TestCaseExpectationEnrichmentInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new TestCaseExpectationEnrichmentInput(
                state.getCanonicalTestCaseBundle(),
                state.getNormalizedRequirementBundle(),
                state.getProjectProfile()
        );
    }

    @Override
    public boolean supports(TestCaseExpectationEnrichmentInput input, WorkflowRunEnvelope run) {
        return input != null
                && input.canonicalTestCaseBundle() != null
                && input.normalizedRequirementBundle() != null;
    }

    @Override
    public TestCaseExpectationEnrichmentOutput execute(TestCaseExpectationEnrichmentInput input, WorkflowRunEnvelope run) {
        CanonicalTestCaseBundle original = input.canonicalTestCaseBundle();
        List<ExpectedResultCandidate> candidates = candidates(input.normalizedRequirementBundle().requirements());
        List<ResolvedExpectedResult> resolved = enrichmentClient.resolve(
                original.testCases(), candidates, input.projectProfile()
        );
        Map<String, ResolvedExpectedResult> byTestCaseId = new LinkedHashMap<>();
        resolved.forEach(result -> byTestCaseId.put(result.testCaseId(), result));
        List<CanonicalTestCase> enrichedCases = original.testCases().stream()
                .map(testCase -> apply(testCase, byTestCaseId.get(testCase.id())))
                .toList();
        CanonicalTestCaseBundle enrichedBundle = new CanonicalTestCaseBundle(
                original.source(), original.primaryPage(), original.pageNames(), enrichedCases
        );
        List<String> failures = enrichmentClient instanceof OpenAiTestCaseExpectationEnrichmentClient openAiClient
                ? openAiClient.lastFailures()
                : List.of();
        return new TestCaseExpectationEnrichmentOutput(enrichedBundle, candidates, resolved, failures);
    }

    @Override
    public void applyOutput(TestCaseExpectationEnrichmentOutput output, WorkflowState state) {
        outputPublisher.publishExpectationEnrichment(output, state);
    }

    private List<ExpectedResultCandidate> candidates(List<NormalizedRequirement> requirements) {
        if (requirements == null) {
            return List.of();
        }
        return requirements.stream()
                .filter(requirement -> requirement.expectedResult() != null && !requirement.expectedResult().isBlank())
                .map(requirement -> new ExpectedResultCandidate(
                        requirement.id(),
                        requirement.expectedResult(),
                        formatSource(requirement)
                ))
                .toList();
    }

    private CanonicalTestCase apply(CanonicalTestCase testCase, ResolvedExpectedResult result) {
        if (result == null || !result.isApproved() || "project-profile-route".equals(result.source())) {
            return testCase;
        }
        List<AssertionIntent> intents = testCase.assertionIntents().stream()
                .map(intent -> intent.kind() == AssertionIntentKind.URL_CONTAINS
                        ? intent
                        : new AssertionIntent(intent.kind(), intent.target(), result.expectedValue()))
                .toList();
        List<String> assertions = intents.stream().map(intent -> assertionText(intent, result.expectedValue())).distinct().toList();
        return new CanonicalTestCase(
                testCase.id(), testCase.title(), testCase.requirementRefs(), testCase.llmSteps(), testCase.operationIntents(),
                intents, testCase.targetPages(), testCase.prerequisiteFlow(), testCase.canonicalFlowId(),
                testCase.canonicalFlowType(), testCase.sourcePageName(), testCase.pageName(), testCase.sourceRoute(),
                testCase.route(), testCase.precondition(), testCase.assertionProfile(), testCase.actions(), assertions,
                testCase.locatorHints(), testCase.sourceReference()
        );
    }

    private String assertionText(AssertionIntent intent, String expectedResult) {
        if (intent.kind() == AssertionIntentKind.URL_CONTAINS) {
            return "Current URL contains " + intent.expectedValue();
        }
        return expectedResult;
    }

    private String formatSource(NormalizedRequirement requirement) {
        if (requirement.sourceReference() == null) {
            return "";
        }
        return requirement.sourceReference().source() + " [L" + requirement.sourceReference().startLine() + "]";
    }
}
