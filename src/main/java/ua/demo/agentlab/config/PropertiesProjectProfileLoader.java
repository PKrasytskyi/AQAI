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
        String profileId = readText("project.profile-id", "default-project");
        String baseUrl = readText(
                "project.base-url",
                readText("ui.base-url", readText("test.base-url", "http://localhost:8080"))
        );
        GenerationNamespace generationNamespace = GenerationNamespace.resolve(
                profileId,
                baseUrl,
                readOptionalText("project.output.generated-pages-package"),
                readOptionalText("project.output.generated-tests-package")
        );
        return new ProjectProfile(
                profileId,
                readText("project.name", "Demo Project"),
                baseUrl,
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
                        generationNamespace.pagesPackage(),
                        generationNamespace.testsPackage()
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
