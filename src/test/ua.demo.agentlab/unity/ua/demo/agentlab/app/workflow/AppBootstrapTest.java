package ua.demo.agentlab.app.workflow;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.orchestration.WorkflowAgent;

public class AppBootstrapTest {

    @Test
    public void bootstrapCreatesDeterministicWorkflowDefinition() {
        WorkflowDefinition definition = new AppBootstrap().createWorkflow(new String[]{"--deterministic"});

        Assert.assertNotNull(definition.initialState());
        Assert.assertFalse(definition.agents().isEmpty());
        Assert.assertEquals(definition.initialState().getArtifacts().get("workflow.mode"), WorkflowMode.DETERMINISTIC.name());
        Assert.assertTrue(definition.agents().stream()
                .map(WorkflowAgent::name)
                .anyMatch("ui-discovery-agent"::equals));
    }

    @Test
    public void bootstrapCreatesApiDemoWorkflowDefinition() {
        WorkflowDefinition definition = new AppBootstrap().createWorkflow(new String[]{"--api"});

        Assert.assertEquals(definition.initialState().getArtifacts().get("workflow.mode"), WorkflowMode.API_DEMO.name());
        Assert.assertTrue(definition.agents().stream()
                .map(WorkflowAgent::name)
                .anyMatch("api-generation-agent"::equals));
        Assert.assertFalse(definition.agents().stream()
                .map(WorkflowAgent::name)
                .anyMatch("ui-discovery-agent"::equals));
    }
}
