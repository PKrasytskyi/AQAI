package unit.tests.app.workflow;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ua.demo.agentlab.app.workflow.WorkflowMode;
import ua.demo.agentlab.app.workflow.WorkflowRequest;
import ua.demo.agentlab.app.workflow.WorkflowRequestFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkflowRequestFactoryDemoTest {

    private static final List<String> KEYS = List.of(
            "project.profile.file",
            "demo.preflight.enabled",
            "demo.execution.active",
            "demo.manifest.file",
            "generated.test.execution.enabled"
    );
    private final Map<String, String> previous = new LinkedHashMap<>();

    @BeforeMethod
    public void rememberProperties() {
        KEYS.forEach(key -> previous.put(key, System.getProperty(key)));
    }

    @AfterMethod
    public void restoreProperties() {
        KEYS.forEach(key -> {
            String value = previous.get(key);
            if (value == null) System.clearProperty(key);
            else System.setProperty(key, value);
        });
    }

    @Test
    public void orangeHrmDemoCommandResolvesImmutableManifestInputs() {
        WorkflowRequest request = new WorkflowRequestFactory().create(new String[]{"--demo", "orangehrm"});

        Assert.assertEquals(request.mode(), WorkflowMode.AI_PROMPT);
        Assert.assertEquals(request.requirementLocation(), "demo/orangehrm-login-logout/requirements.md");
        Assert.assertEquals(request.projectProfile().profileId(), "orangeHRM");
        Assert.assertEquals(request.projectProfile().loginRoute(), "/auth/login");
        Assert.assertEquals(System.getProperty("demo.manifest.file").replace('\\', '/'),
                PathSupport.absolute("demo/orangehrm-login-logout/demo-manifest.yaml"));
        Assert.assertEquals(System.getProperty("generated.test.execution.enabled"), "true");
        Assert.assertEquals(System.getProperty("demo.execution.active"), "true");
    }

    private static final class PathSupport {
        private static String absolute(String value) {
            return java.nio.file.Path.of(value).toAbsolutePath().normalize().toString().replace('\\', '/');
        }
    }
}
