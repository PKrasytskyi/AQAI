package ua.demo.agentlab.futurefeat.testplan.agent;

import ua.demo.agentlab.futurefeat.testplan.generator.TestPlanGenerator;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

import java.util.Set;

public class TestPlanAgent implements WorkflowAgent,
        PipelineAgent<TestPlanGenerationInput, TestPlan> {

    private final TestPlanGenerator testPlanGenerator;
    private final StageOutputPublisher publisher = new StageOutputPublisher();

    public TestPlanAgent(TestPlanGenerator testPlanGenerator) {
        this.testPlanGenerator = testPlanGenerator;
    }

    @Override
    public String name() {
        return "test-plan-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.TEST_PLAN);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.TEST_PLAN;
    }

    @Override
    public TestPlanGenerationInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        NormalizedRequirementBundle bundle = store.require(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE);
        WorkflowRunEnvelope run = WorkflowRunEnvelope.from(state);
        return new TestPlanGenerationInput(bundle, run.objective(), run.requirementInput());
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return store != null
                && store.get(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE).isPresent()
                && store.get(WorkflowArtifact.TEST_PLAN).isEmpty();
    }

    @Override
    public TestPlan execute(TestPlanGenerationInput input, WorkflowRunEnvelope run) {
        WorkflowState generatorContext = new WorkflowState(input.objective(), input.requirementInput());
        generatorContext.setNormalizedRequirementBundle(input.bundle());
        return testPlanGenerator.generate(input.bundle(), generatorContext);
    }

    @Override
    public void applyOutput(TestPlan testPlan, WorkflowState state) {
        publisher.publishTestPlan(testPlan, state);
    }
}
