package ua.demo.agentlab.ui.testcontract.agent;

import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;
import ua.demo.agentlab.ui.testcontract.validation.UiTestContractValidationInput;
import ua.demo.agentlab.ui.testcontract.validation.UiTestContractValidationResult;
import ua.demo.agentlab.ui.testcontract.validation.UiTestContractValidator;

import java.util.List;
import java.util.Set;

public class UiTestContractValidationAgent implements WorkflowAgent,
        PipelineAgent<UiTestContractValidationInput, UiTestContractValidationResult> {

    private final UiTestContractValidator validator;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public UiTestContractValidationAgent(UiTestContractValidator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("validator cannot be null");
        }
        this.validator = validator;
    }

    @Override
    public String name() {
        return "ui-test-contract-validation-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE,
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.POM_CONTRACT_SPECS
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.UI_TEST_CONTRACT_VALIDATION,
                WorkflowArtifact.UI_TEST_CONTRACT_SCHEMA_VALIDATION,
                WorkflowArtifact.UI_TEST_CONTRACT_QUALITY_REPORT
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_TEST_CONTRACT_VALIDATION;
    }

    @Override
    public UiTestContractValidationInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        UiTestContractBundle bundle = store.require(WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE);
        CanonicalTestCaseBundle canonical = store.require(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE);
        List<PomContractSpec> pomContracts = store.getList(WorkflowArtifact.POM_CONTRACT_SPECS, PomContractSpec.class);
        return new UiTestContractValidationInput(bundle, canonical, pomContracts);
    }

    @Override
    public UiTestContractValidationResult execute(UiTestContractValidationInput input, WorkflowRunEnvelope run) {
        return validator.validate(input);
    }

    @Override
    public void applyOutput(UiTestContractValidationResult output, WorkflowState state) {
        artifactPublisher.writeJson(
                state,
                "validation",
                "ui-test-contract-schema-validation.json",
                output.schemaReport()
        );
        artifactPublisher.writeJson(
                state,
                "validation",
                "ui-test-contract-quality-report.json",
                output.qualityReport()
        );
        if (!output.qualityReport().issues().isEmpty()) {
            artifactPublisher.writeJson(
                    state,
                    "need-review",
                    "ui-test-contract-needs-review.json",
                    output.qualityReport().issues()
            );
        }
        state.addArtifact("ui.test.contract.schema.valid", String.valueOf(output.schemaReport().valid()));
        state.addArtifact("ui.test.contract.quality.valid", String.valueOf(!output.qualityReport().hasBlockingIssues()));
        state.addFinding("Typed UI test contract validation: " + (output.valid() ? "PASSED" : "FAILED"));
    }
}
