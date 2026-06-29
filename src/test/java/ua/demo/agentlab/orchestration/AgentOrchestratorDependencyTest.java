package ua.demo.agentlab.orchestration;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.orchestration.pipeline.WorkflowStatePipelineAdapter;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

import java.util.List;
import java.util.Set;

public class AgentOrchestratorDependencyTest {

    @Test
    public void orchestratorUsesDependenciesBeforeNumericOrder() {
        WorkflowAgent consumer = new TestAgent(
                "consumer",
                1,
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                Set.of(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE),
                state -> state.addArtifact("execution.order", state.getArtifacts().get("execution.order") + ">consumer")
        );
        WorkflowAgent producer = new TestAgent(
                "producer",
                99,
                Set.of(),
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                state -> state.addArtifact("execution.order", "producer")
        );

        WorkflowState state = new WorkflowState("test", new RequirementInput(SourceType.FILE, "requirements.md"));
        new AgentOrchestrator(List.of(consumer, producer)).run(state);

        Assert.assertEquals(state.getArtifacts().get("execution.order"), "producer>consumer");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void orchestratorRejectsDependencyCycles() {
        WorkflowAgent left = new TestAgent(
                "left",
                1,
                Set.of(WorkflowArtifact.AI_CONTEXT_PACKAGE),
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                state -> { }
        );
        WorkflowAgent right = new TestAgent(
                "right",
                1,
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                Set.of(WorkflowArtifact.AI_CONTEXT_PACKAGE),
                state -> { }
        );

        new AgentOrchestrator(List.of(left, right));
    }

    @Test
    public void orchestratorExecutesMigratedAgentThroughTypedPipelinePath() {
        WorkflowState state = new WorkflowState("test", new RequirementInput(SourceType.FILE, "requirements.md"));
        state.setRequirementDocument(new RequirementDocument("requirements.md", "REQ-001"));

        new AgentOrchestrator(List.of(new TypedOnlyNormalizationAgent())).run(state);

        Assert.assertFalse(state.isFailed(), state.getFailureReason());
        Assert.assertNotNull(state.getNormalizedRequirementBundle());
        Assert.assertEquals(state.getNormalizedRequirementBundle().source(), "typed");
        Assert.assertEquals(state.getArtifacts().get("typed.pipeline.used"), "true");
    }

    @Test
    public void orchestratorFallsBackToLegacyWorkflowAgentPath() {
        WorkflowState state = new WorkflowState("test", new RequirementInput(SourceType.FILE, "requirements.md"));

        new AgentOrchestrator(List.of(new TestAgent(
                "legacy",
                1,
                Set.of(),
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                workflowState -> workflowState.addArtifact("legacy.pipeline.used", "true")
        ))).run(state);

        Assert.assertEquals(state.getArtifacts().get("legacy.pipeline.used"), "true");
    }

    private record TestAgent(
            String name,
            int order,
            Set<WorkflowArtifact> requires,
            Set<WorkflowArtifact> produces,
            java.util.function.Consumer<WorkflowState> action
    ) implements WorkflowAgent {

        @Override
        public boolean supports(WorkflowState state) {
            return true;
        }

        @Override
        public void execute(WorkflowState state) {
            action.accept(state);
        }
    }

    private static final class TypedOnlyNormalizationAgent implements WorkflowAgent,
            PipelineAgent<RequirementDocument, NormalizedRequirementBundle>,
            WorkflowStatePipelineAdapter<NormalizedRequirementBundle> {

        @Override
        public String name() {
            return "typed-only-normalization-agent";
        }

        @Override
        public int order() {
            return 1;
        }

        @Override
        public Set<WorkflowArtifact> requires() {
            return Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT);
        }

        @Override
        public Set<WorkflowArtifact> produces() {
            return Set.of(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE);
        }

        @Override
        public WorkflowArtifact input() {
            return WorkflowArtifact.REQUIREMENT_DOCUMENT;
        }

        @Override
        public WorkflowArtifact output() {
            return WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE;
        }

        @Override
        public boolean supports(WorkflowState state) {
            return state.getRequirementDocument() != null;
        }

        @Override
        public void execute(WorkflowState state) {
            throw new AssertionError("legacy execute must not be called for PipelineAgent");
        }

        @Override
        public NormalizedRequirementBundle execute(RequirementDocument input, WorkflowRunEnvelope run) {
            return new NormalizedRequirementBundle("typed", List.of(), List.of(), List.of());
        }

        @Override
        public void applyOutput(NormalizedRequirementBundle output, WorkflowState state) {
            state.setNormalizedRequirementBundle(output);
            state.addArtifact("typed.pipeline.used", "true");
        }
    }
}
