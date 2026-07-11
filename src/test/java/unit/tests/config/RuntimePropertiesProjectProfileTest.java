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

