package unit.tests.app.workflow;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.app.workflow.AppBootstrap;
import ua.demo.agentlab.app.workflow.AiPromptWorkflowFactory;
import ua.demo.agentlab.app.workflow.WorkflowCoreComponents;
import ua.demo.agentlab.app.workflow.WorkflowCoreModuleFactory;
import ua.demo.agentlab.app.workflow.WorkflowDefinition;
import ua.demo.agentlab.app.workflow.WorkflowMode;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;

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
        Assert.assertFalse(definition.agents().stream()
                .map(WorkflowAgent::name)
                .anyMatch("api-generation-agent"::equals));
        Assert.assertFalse(definition.agents().stream()
                .map(WorkflowAgent::name)
                .anyMatch("api-generated-source-persistence-agent"::equals));
    }

    @Test
    public void bootstrapCreatesAiPromptWorkflowWithoutApiGenerationAgents() {
        WorkflowState state = new WorkflowState(
                "test",
                new RequirementInput(SourceType.FILE, "requirements/valid-author.md")
        );
        WorkflowCoreComponents core = new WorkflowCoreModuleFactory().create(testProfile());
        WorkflowDefinition definition = new AiPromptWorkflowFactory().create(state, core);

        Assert.assertTrue(definition.agents().stream()
                .map(WorkflowAgent::name)
                .anyMatch("ai-page-object-spec-agent"::equals));
        Assert.assertFalse(definition.agents().stream()
                .map(WorkflowAgent::name)
                .anyMatch("api-generation-agent"::equals));
        Assert.assertFalse(definition.agents().stream()
                .map(WorkflowAgent::name)
                .anyMatch("api-generated-source-persistence-agent"::equals));
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

    private ProjectProfile testProfile() {
        return new ProjectProfile(
                "test",
                "Test App",
                "https://example.test",
                "/",
                "/login",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new OutputProfile("generated.pages", "generated.tests")
        );
    }
}
