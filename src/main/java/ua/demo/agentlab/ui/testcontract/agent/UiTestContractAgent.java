package ua.demo.agentlab.ui.testcontract.agent;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.testcontract.assembly.UiTestContractAssembler;
import ua.demo.agentlab.ui.testcontract.assembly.UiTestContractAssemblyInput;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;

import java.util.List;
import java.util.Set;

public class UiTestContractAgent implements WorkflowAgent,
        PipelineAgent<UiTestContractAssemblyInput, UiTestContractBundle> {

    private final UiTestContractAssembler assembler;
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();

    public UiTestContractAgent(UiTestContractAssembler assembler) {
        if (assembler == null) {
            throw new IllegalArgumentException("assembler cannot be null");
        }
        this.assembler = assembler;
    }

    @Override
    public String name() {
        return "ui-test-contract-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.ASSERTION_CONTRACTS,
                WorkflowArtifact.POM_CONTRACT_SPECS
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.UI_TEST_CONTRACT_BUNDLE;
    }

    @Override
    public UiTestContractAssemblyInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        CanonicalTestCaseBundle canonical = store.require(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE);
        List<AssertionContract> assertions = store.getList(WorkflowArtifact.ASSERTION_CONTRACTS, AssertionContract.class);
        List<PomContractSpec> pomContracts = store.getList(WorkflowArtifact.POM_CONTRACT_SPECS, PomContractSpec.class);
        return new UiTestContractAssemblyInput(canonical, assertions, pomContracts);
    }

    @Override
    public boolean supports(UiTestContractAssemblyInput input, WorkflowRunEnvelope run) {
        return input != null
                && input.canonicalTestCases() != null
                && !input.canonicalTestCases().testCases().isEmpty()
                && !input.pomContracts().isEmpty();
    }

    @Override
    public UiTestContractBundle execute(UiTestContractAssemblyInput input, WorkflowRunEnvelope run) {
        return assembler.assemble(input);
    }

    @Override
    public void applyOutput(UiTestContractBundle output, WorkflowState state) {
        artifactPublisher.writeJson(state, "test-spec", "ui-test-contracts.json", output);
        state.addArtifact("ui.test.contract.count", String.valueOf(output.contracts().size()));
        state.addFinding("Typed UI test contracts assembled: " + output.contracts().size());
    }
}
