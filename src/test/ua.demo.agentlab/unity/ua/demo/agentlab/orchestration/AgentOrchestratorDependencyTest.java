package ua.demo.agentlab.orchestration;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

import java.util.List;
import java.util.Set;

public class AgentOrchestratorDependencyTest {

    @Test
    public void orchestratorUsesDependenciesBeforeInputOrder() {
        WorkflowAgent consumer = new TestAgent(
                "consumer",
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                Set.of(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE),
                state -> state.addArtifact("execution.order", state.getArtifacts().get("execution.order") + ">consumer"),
                new NormalizedRequirementBundle("consumer", List.of(), List.of(), List.of())
        );
        WorkflowAgent producer = new TestAgent(
                "producer",
                Set.of(),
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                state -> state.addArtifact("execution.order", "producer"),
                new RequirementDocument("requirements.md", "REQ-001")
        );

        WorkflowState state = new WorkflowState("test", new RequirementInput(SourceType.FILE, "requirements.md"));
        new AgentOrchestrator(List.of(consumer, producer)).run(state);

        Assert.assertEquals(state.getArtifacts().get("execution.order"), "producer>consumer");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void orchestratorRejectsDependencyCycles() {
        WorkflowAgent left = new TestAgent(
                "left",
                Set.of(WorkflowArtifact.AI_CONTEXT_PACKAGE),
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                state -> { },
                new RequirementDocument("left.md", "left")
        );
        WorkflowAgent right = new TestAgent(
                "right",
                Set.of(WorkflowArtifact.REQUIREMENT_DOCUMENT),
                Set.of(WorkflowArtifact.AI_CONTEXT_PACKAGE),
                state -> { },
                new Object()
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

    @Test(expectedExceptions = IllegalStateException.class)
    public void orchestratorRejectsNonPipelineAgents() {
        WorkflowState state = new WorkflowState("test", new RequirementInput(SourceType.FILE, "requirements.md"));

        WorkflowAgent nonPipeline = new WorkflowAgent() {
            @Override
            public String name() {
                return "non-pipeline";
            }
        };

        new AgentOrchestrator(List.of(nonPipeline)).run(state);
    }

    private record TestAgent(
            String name,
            Set<WorkflowArtifact> requires,
            Set<WorkflowArtifact> produces,
            java.util.function.Consumer<WorkflowState> action,
            Object outputValue
    ) implements WorkflowAgent, PipelineAgent<Object, Object> {

        @Override
        public WorkflowArtifact input() {
            return requires.isEmpty() ? WorkflowArtifact.REQUIREMENT_INPUT : requires.iterator().next();
        }

        @Override
        public WorkflowArtifact output() {
            return produces.isEmpty() ? WorkflowArtifact.REQUIREMENT_DOCUMENT : produces.iterator().next();
        }

        @Override
        public boolean supports(PipelineArtifactStore store, WorkflowState state) {
            return true;
        }

        @Override
        public Object inputFrom(PipelineArtifactStore store, WorkflowState state) {
            return new Object();
        }

        @Override
        public Object execute(Object input, WorkflowRunEnvelope run) {
            return outputValue;
        }

        @Override
        public void applyOutput(Object output, WorkflowState state) {
            action.accept(state);
        }
    }

    private static final class TypedOnlyNormalizationAgent implements WorkflowAgent,
            PipelineAgent<RequirementDocument, NormalizedRequirementBundle> {

        @Override
        public String name() {
            return "typed-only-normalization-agent";
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
