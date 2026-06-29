package ua.demo.agentlab.ai.assertions.agent;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.assertions.service.AssertionContractBuilder;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.orchestration.pipeline.WorkflowStatePipelineAdapter;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.util.List;
import java.util.Set;

public class AssertionContractAgent implements WorkflowAgent,
        PipelineAgent<CanonicalTestCaseBundle, List<AssertionContract>>,
        WorkflowStatePipelineAdapter<List<AssertionContract>> {

    private final AssertionContractBuilder contractBuilder;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public AssertionContractAgent() {
        this(new AssertionContractBuilder());
    }

    public AssertionContractAgent(AssertionContractBuilder contractBuilder) {
        this.contractBuilder = contractBuilder == null ? new AssertionContractBuilder() : contractBuilder;
    }

    @Override
    public String name() {
        return "assertion-contract-agent";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.ASSERTION_CONTRACTS);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.ASSERTION_CONTRACTS;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getCanonicalTestCaseBundle() != null && state.getAssertionContracts().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        List<AssertionContract> contracts = execute(state.getCanonicalTestCaseBundle(), WorkflowRunEnvelope.from(state));
        applyOutput(contracts, state);
    }

    @Override
    public void applyOutput(List<AssertionContract> contracts, WorkflowState state) {
        outputPublisher.publishAssertionContracts(contracts, state);
    }

    @Override
    public List<AssertionContract> execute(CanonicalTestCaseBundle input, WorkflowRunEnvelope run) {
        if (input == null) {
            throw new IllegalArgumentException("canonical test case bundle cannot be null");
        }
        return contractBuilder.build(input.testCases());
    }
}
