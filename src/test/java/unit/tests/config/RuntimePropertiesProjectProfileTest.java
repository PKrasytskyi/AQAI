package unit.tests.config;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.RuntimeProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class RuntimePropertiesProjectProfileTest {

    @Test
    public void activeFrameworkConfigurationResolvesFrozenOrangeHrmDemoBundle() {
        RuntimeProperties runtimeProperties = new RuntimeProperties();
        PropertiesProjectProfileLoader loader = new PropertiesProjectProfileLoader(runtimeProperties);
        ProjectProfile profile = loader.loadDefaultProfile();

        Assert.assertEquals(runtimeProperties.profileFile(),
                "demo/orangehrm-login-logout/project-profile.yaml");
        Assert.assertEquals(profile.profileId(), "orangeHRM");
        Assert.assertEquals(profile.projectName(), "OrangeHRM Login Logout Demo");
        Assert.assertEquals(loader.defaultRequirementLocation(),
                "demo/orangehrm-login-logout/requirements.md");
        Assert.assertTrue(runtimeProperties.readBoolean("demo.preflight.enabled", "false"));
        Assert.assertEquals(runtimeProperties.readValue("demo.manifest.file", ""),
                "demo/orangehrm-login-logout/demo-manifest.yaml");
    }

    @Test
    public void orangeHrmProfileResolvesAuthenticationUserMenuLogoutFixture() {
        Properties framework = new Properties();
        framework.setProperty("project.profile.file", "profiles/orangehrm.project-profile.yaml");
        framework.setProperty("project.output.generated-pages-package", "ua.demo.agentlab.ui.generated.pages");
        framework.setProperty("project.output.generated-tests-package", "ua.demo.agentlab.ui.generated.tests");

        RuntimeProperties runtimeProperties = new RuntimeProperties(framework);
        PropertiesProjectProfileLoader loader = new PropertiesProjectProfileLoader(runtimeProperties);
        ProjectProfile profile = loader.loadDefaultProfile();

        Assert.assertEquals(profile.profileId(), "orangeHRM");
        Assert.assertEquals(profile.baseUrl(), "https://opensource-demo.orangehrmlive.com/web/index.php");
        Assert.assertEquals(profile.homeRoute(), "/auth/login");
        Assert.assertEquals(profile.loginRoute(), "/auth/login");
        Assert.assertEquals(profile.authenticatedRoute(), "/dashboard/index");
        Assert.assertEquals(loader.defaultRequirementLocation(),
                "requirements/orangehrm-authentication-user-menu-logout.md");
        Assert.assertEquals(runtimeProperties.readValue("discovery.auth.submit-selector", ""),
                "button[type='submit'], input[type='submit']");
        Assert.assertTrue(runtimeProperties.readBoolean(
                "spa.live-verification.execute-session-ending-actions", "false"));
        Assert.assertTrue(runtimeProperties.readBoolean("spa.live-verification.execute-safe-actions", "false"));
        Assert.assertFalse(runtimeProperties.readBoolean("spa.live-verification.execute-data-actions", "true"));
    }

    @Test
    public void profileValuesOverrideFrameworkAndSystemPropertiesOverrideProfile() throws Exception {
        Path profile = Files.createTempFile("project-profile", ".yaml");
        Files.writeString(profile, """
                schemaVersion: project-profile.v1
                project:
                  id: profile-app
                  name: Profile App
                  baseUrl: https://profile.example
                requirements:
                  file: requirements/profile.md
                routes:
                  login: /profile-login
                  authenticated: /profile-dashboard
                ui:
                  browser: firefox
                  headless: false
                  timeoutSeconds: 15
                ai:
                  openAiEnabled: true
                knowledge:
                  ragEnabled: true
                  graphEnabled: true
                  vectorEnabled: true
                """);

        Properties framework = new Properties();
        framework.setProperty("project.profile.file", profile.toString());
        framework.setProperty("project.profile-id", "framework-app");
        framework.setProperty("project.name", "Framework App");
        framework.setProperty("project.base-url", "https://framework.example");
        framework.setProperty("project.route.login", "/framework-login");
        framework.setProperty("project.output.generated-pages-package", "pages");
        framework.setProperty("project.output.generated-tests-package", "tests");

        RuntimeProperties runtimeProperties = new RuntimeProperties(framework);
        PropertiesProjectProfileLoader loader = new PropertiesProjectProfileLoader(runtimeProperties);
        ProjectProfile profileResult = loader.loadDefaultProfile();

        Assert.assertEquals(profileResult.profileId(), "profile-app");
        Assert.assertEquals(profileResult.projectName(), "Profile App");
        Assert.assertEquals(profileResult.baseUrl(), "https://profile.example");
        Assert.assertEquals(profileResult.loginRoute(), "/profile-login");
        Assert.assertEquals(profileResult.authenticatedRoute(), "/profile-dashboard");
        Assert.assertEquals(loader.defaultRequirementLocation(), "requirements/profile.md");
        Assert.assertEquals(runtimeProperties.readValue("ui.browser", "chrome"), "firefox");
        Assert.assertTrue(runtimeProperties.readKnowledgeDbBoolean("knowledge.graph.enabled", "false"));

        System.setProperty("project.name", "System App");
        System.setProperty("knowledge.db.status", "false");
        try {
            ProjectProfile systemOverride = loader.loadDefaultProfile();
            Assert.assertEquals(systemOverride.projectName(), "System App");
            Assert.assertFalse(runtimeProperties.readKnowledgeDbBoolean("knowledge.graph.enabled", "true"));
            Assert.assertFalse(runtimeProperties.readKnowledgeDbBoolean("knowledge.vector.enabled", "true"));
            Assert.assertFalse(runtimeProperties.readKnowledgeDbBoolean("rag.enabled", "true"));
        } finally {
            System.clearProperty("project.name");
            System.clearProperty("knowledge.db.status");
        }
    }
}
