package ua.demo.agentlab.app.workflow;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;

public class WorkflowCoreModuleFactoryTest {

    @Test
    public void coreFactoryComposesSharedWorkflowModules() {
        ProjectProfile profile = new ProjectProfile(
                "test",
                "Test Project",
                "https://example.test",
                "/",
                "/login",
                "",
                "/secure",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new OutputProfile(
                        "ua.demo.agentlab.ui.generated.pages",
                        "ua.demo.agentlab.ui.generated.tests"
                )
        );

        WorkflowCoreComponents components = new WorkflowCoreModuleFactory().create(profile);

        Assert.assertNotNull(components.seleniumWriter());
        Assert.assertNotNull(components.generatedFileWriter());
        Assert.assertNotNull(components.canonicalInteractionLayer());
        Assert.assertEquals(components.uiDiscoveryAgent().name(), "ui-discovery-agent");
        Assert.assertEquals(components.apiGenerationAgent().name(), "api-generation-agent");
        Assert.assertEquals(components.generatedCodeCompileAgent().name(), "generated-code-compile-agent");
    }
}
