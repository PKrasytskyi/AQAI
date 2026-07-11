package ua.demo.agentlab.config;

public class PropertiesProjectProfileLoader implements ProjectProfileLoader {

    private final RuntimeProperties properties;

    public PropertiesProjectProfileLoader() {
        this("framework.properties");
    }

    public PropertiesProjectProfileLoader(String resourceName) {
        this(new RuntimeProperties(resourceName));
    }

    public PropertiesProjectProfileLoader(RuntimeProperties properties) {
        this.properties = properties == null ? new RuntimeProperties() : properties;
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

    @Override
    public String defaultRequirementLocation() {
        return readText("project.requirements.file", "");
    }

    private String readText(String key, String fallback) {
        return properties.readValue(key, fallback);
    }

    private String readOptionalText(String key) {
        return readText(key, "");
    }
}
