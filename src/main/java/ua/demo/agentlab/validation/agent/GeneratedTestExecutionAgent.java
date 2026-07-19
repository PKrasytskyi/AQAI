package ua.demo.agentlab.validation.agent;

import ua.demo.agentlab.config.RuntimeProperties;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.persistence.GeneratedSourceManifest;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.review.ReviewSeverity;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionResult;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionService;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.ui.testcontract.writer.UiTestSourceMap;

import java.util.Set;

/** Runs deterministic tests only after source, compile, review, and generated-source smoke gates are green. */
public final class GeneratedTestExecutionAgent implements WorkflowAgent,
        PipelineAgent<GeneratedTestExecutionAgent.Input, GeneratedTestExecutionResult> {

    private final GeneratedTestExecutionService service;
    private final RuntimeProperties properties;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public GeneratedTestExecutionAgent(GeneratedTestExecutionService service, RuntimeProperties properties) {
        if (service == null || properties == null) {
            throw new IllegalArgumentException("generated test execution dependencies cannot be null");
        }
        this.service = service;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "generated-test-execution-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.GENERATED_SOURCE_MANIFEST,
                WorkflowArtifact.COMPILE_RESULT,
                WorkflowArtifact.REVIEW_RESULT,
                WorkflowArtifact.GENERATED_UI_SMOKE_RESULT,
                WorkflowArtifact.UI_TEST_SOURCE_MAP
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATED_TEST_EXECUTION_RESULT);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.GENERATED_SOURCE_MANIFEST;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_TEST_EXECUTION_RESULT;
    }

    @Override
    public Input inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return new Input(
                store.require(WorkflowArtifact.GENERATED_SOURCE_MANIFEST),
                store.require(WorkflowArtifact.COMPILE_RESULT),
                store.require(WorkflowArtifact.REVIEW_RESULT),
                store.require(WorkflowArtifact.GENERATED_UI_SMOKE_RESULT),
                store.require(WorkflowArtifact.UI_TEST_SOURCE_MAP),
                executionRequired()
        );
    }

    @Override
    public GeneratedTestExecutionResult execute(Input input, WorkflowRunEnvelope run) {
        var testClasses = input.manifest().files().stream()
                .filter(entry -> entry.kind() == ua.demo.agentlab.persistence.GeneratedSourceKind.UI_TEST)
                .map(entry -> entry.packageName() + "." + entry.className())
                .toList();
        if (!input.required()) {
            return GeneratedTestExecutionResult.skipped(
                    testClasses,
                    "Generated test execution is disabled for this run"
            );
        }
        if (!input.compileResult().isPassed()) {
            return GeneratedTestExecutionResult.blocked(
                    testClasses, "Generated tests cannot run because compile gate is not green");
        }
        long criticalFindings = input.reviewReport().findings().stream()
                .filter(finding -> finding.severity() == ReviewSeverity.CRITICAL)
                .count();
        if (criticalFindings > 0) {
            return GeneratedTestExecutionResult.blocked(
                    testClasses, "Generated tests cannot run because code review has "
                            + criticalFindings + " critical finding(s)");
        }
        if (!input.smokeResult().passed()) {
            return GeneratedTestExecutionResult.blocked(
                    testClasses, "Generated tests cannot run because generated UI smoke is not green");
        }
        return service.execute(input.manifest(), input.sourceMap());
    }

    @Override
    public void applyOutput(GeneratedTestExecutionResult output, WorkflowState state) {
        artifactPublisher.writeJson(state, "validation", "generated-tests-execution-result.json", output);
        state.addArtifact("generated.tests.execution.status", output.status().name());
        state.addArtifact("generated.tests.execution.total", String.valueOf(output.total()));
        state.addArtifact("generated.tests.execution.passed", String.valueOf(output.passed()));
        state.addArtifact("generated.tests.execution.failed", String.valueOf(output.failed()));
        state.addFinding(output.summary());
        // Terminal demo acceptance fails only after negative runtime feedback is persisted.
    }

    private boolean executionRequired() {
        String explicit = properties.readValue("generated.test.execution.enabled", "");
        if (!explicit.isBlank()) {
            return Boolean.parseBoolean(explicit);
        }
        return false;
    }

    public record Input(
            GeneratedSourceManifest manifest,
            GeneratedCodeValidationResult compileResult,
            GeneratedCodeReviewReport reviewReport,
            GeneratedUiSmokeResult smokeResult,
            UiTestSourceMap sourceMap,
            boolean required
    ) {
    }
}
