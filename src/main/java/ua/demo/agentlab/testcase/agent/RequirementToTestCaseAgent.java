package ua.demo.agentlab.testcase.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseInput;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseGenerator;
import ua.demo.agentlab.testcase.governance.RequirementGovernancePartitioner;

import java.util.Set;

public class RequirementToTestCaseAgent implements WorkflowAgent,
        PipelineAgent<RequirementToTestCaseInput, RequirementToTestCaseOutput> {

    private final RequirementToTestCaseGenerator generator;
    private final RequirementGovernancePartitioner governancePartitioner;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public RequirementToTestCaseAgent(RequirementToTestCaseGenerator generator) {
        this(generator, new RequirementGovernancePartitioner());
    }

    RequirementToTestCaseAgent(
            RequirementToTestCaseGenerator generator,
            RequirementGovernancePartitioner governancePartitioner
    ) {
        this.generator = generator;
        this.governancePartitioner = governancePartitioner;
    }

    @Override
    public String name() {
        return "requirement-to-test-case-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE,
                WorkflowArtifact.MAPPED_UI_KNOWLEDGE
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.REQUIREMENT_TO_TEST_CASE_OUTPUT,
                WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE,
                WorkflowArtifact.REQUIREMENT_GOVERNANCE_BUNDLE
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.REQUIREMENT_TO_TEST_CASE_OUTPUT;
    }

    @Override
    public RequirementToTestCaseInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new RequirementToTestCaseInput(
                state.getProjectProfile(),
                state.getNormalizedRequirementBundle(),
                state.getMappedUiKnowledge(),
                state.getFlowScopedKnowledgePackage()
        );
    }

    @Override
    public boolean supports(RequirementToTestCaseInput input, WorkflowRunEnvelope run) {
        return input != null && input.normalizedRequirementBundle() != null;
    }

    @Override
    public RequirementToTestCaseOutput execute(RequirementToTestCaseInput input, WorkflowRunEnvelope run) {
        return new RequirementToTestCaseOutput(
                generator.generate(input),
                governancePartitioner.partition(input.normalizedRequirementBundle())
        );
    }

    @Override
    public void applyOutput(RequirementToTestCaseOutput output, WorkflowState state) {
        outputPublisher.publishRequirementToTestCaseOutput(output, state);
    }
}
