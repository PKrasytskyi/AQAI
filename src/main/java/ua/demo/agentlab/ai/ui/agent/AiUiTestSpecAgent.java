package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.ui.generation.AiUiTestSpecGenerator;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;

public class AiUiTestSpecAgent implements WorkflowAgent {

    private final AiUiTestSpecGenerator generator;

    public AiUiTestSpecAgent(AiUiTestSpecGenerator generator) {
        if (generator == null) {
            throw new IllegalArgumentException("generator cannot be null");
        }
        this.generator = generator;
    }

    @Override
    public String name() {
        return "ai-ui-test-spec-agent";
    }

    @Override
    public int order() {
        return 45;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getUiTestPlan() != null && state.getAiUiTestSpecs().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {
        state.setAiUiTestSpecs(generator.generate(state, state.getAiPageObjectSpecs(), state.getAiUiTestSpecs()));
        state.addArtifact("ai.ui.test.spec.count", String.valueOf(state.getAiUiTestSpecs().size()));
    }
}
