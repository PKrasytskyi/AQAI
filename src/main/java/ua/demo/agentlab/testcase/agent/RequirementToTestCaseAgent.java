package ua.demo.agentlab.testcase.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.orchestration.pipeline.WorkflowStatePipelineAdapter;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseInput;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseGenerator;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.util.Set;

public class RequirementToTestCaseAgent implements WorkflowAgent,
        PipelineAgent<RequirementToTestCaseInput, CanonicalTestCaseBundle>,
        WorkflowStatePipelineAdapter<CanonicalTestCaseBundle> {

    private final RequirementToTestCaseGenerator generator;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public RequirementToTestCaseAgent(RequirementToTestCaseGenerator generator) {
        this.generator = generator;
    }

    @Override
    public String name() {
        return "requirement-to-test-case-agent";
    }

    @Override
    public int order() {
        return 28;
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
        return Set.of(WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getNormalizedRequirementBundle() != null && state.getCanonicalTestCaseBundle() == null;
    }

    @Override
    public void execute(WorkflowState state) {
        applyOutput(execute(inputFrom(null, state), WorkflowRunEnvelope.from(state)), state);
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
    public CanonicalTestCaseBundle execute(RequirementToTestCaseInput input, WorkflowRunEnvelope run) {
        return generator.generate(input);
    }

    @Override
    public void applyOutput(CanonicalTestCaseBundle output, WorkflowState state) {
        outputPublisher.publishCanonicalTestCaseBundle(output, state);
    }
}
