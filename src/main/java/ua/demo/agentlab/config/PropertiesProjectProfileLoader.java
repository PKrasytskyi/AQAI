package ua.demo.agentlab.config;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesProjectProfileLoader implements ProjectProfileLoader {

    private final Properties properties = new Properties();

    public PropertiesProjectProfileLoader() {
        this("framework.properties");
    }

    public PropertiesProjectProfileLoader(String resourceName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Cannot find project profile resource: " + resourceName);
            }
            properties.load(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load project profile resource: " + resourceName, exception);
        }
    }

    @Override
    public ProjectProfile loadDefaultProfile() {
        return new ProjectProfile(
                readText("project.profile-id", "default-project"),
                readText("project.name", "Demo Project"),
                readText("project.base-url", readText("ui.base-url", readText("test.base-url", "http://localhost:8080"))),
                readOptionalText("project.route.home"),
                readOptionalText("project.route.login"),
                readOptionalText("project.route.registration"),
                readOptionalText("project.route.authenticated"),
                readOptionalText("project.route.recovery"),
                readOptionalText("project.route.details"),
                readOptionalText("project.route.form"),
                readOptionalText("project.route.security"),
                readOptionalText("project.route.catalog"),
                readOptionalText("project.route.products"),
                readOptionalText("project.route.cart"),
                new OutputProfile(
                        readText("project.output.generated-pages-package", "ua.demo.agentlab.ui.generated.pages"),
                        readText("project.output.generated-tests-package", "ua.demo.agentlab.ui.generated.tests")
                )
        );
    }

    private String readText(String key, String fallback) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return fallback;
    }

    private String readOptionalText(String key) {
        return readText(key, "");
    }
}
