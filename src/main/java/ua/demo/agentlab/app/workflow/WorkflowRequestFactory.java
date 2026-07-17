package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.ProjectProfileLoader;
import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.requirements.model.SourceType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class WorkflowRequestFactory {

    private static final String DEFAULT_REQUIREMENT_LOCATION = "requirements/valid-login-requirement.md";

    private final ProjectProfileLoader projectProfileLoader;
    private final WorkflowModeResolver modeResolver;

    public WorkflowRequestFactory() {
        this(new PropertiesProjectProfileLoader(), new WorkflowModeResolver());
    }

    WorkflowRequestFactory(ProjectProfileLoader projectProfileLoader, WorkflowModeResolver modeResolver) {
        if (projectProfileLoader == null || modeResolver == null) {
            throw new IllegalArgumentException("workflow request collaborators cannot be null");
        }
        this.projectProfileLoader = projectProfileLoader;
        this.modeResolver = modeResolver;
    }

    public WorkflowRequest create(String[] args) {
        ProjectProfile projectProfile = projectProfileLoader.loadDefaultProfile();
        String requirementLocation = resolveRequirementLocation(args, projectProfileLoader.defaultRequirementLocation());
        SourceType sourceType = detectSourceType(requirementLocation);
        return new WorkflowRequest(
                requirementLocation,
                sourceType,
                modeResolver.resolve(args),
                projectProfile
        );
    }

    private String resolveRequirementLocation(String[] args, String profileRequirementLocation) {
        String requestedLocation = Arrays.stream(args == null ? new String[0] : args)
                .filter(arg -> !arg.startsWith("--"))
                .findFirst()
                .orElseGet(() -> profileRequirementLocation == null || profileRequirementLocation.isBlank()
                        ? DEFAULT_REQUIREMENT_LOCATION
                        : profileRequirementLocation.trim());

        if (isMissingFileLocation(requestedLocation)) {
            throw new IllegalArgumentException("Requirement file not found: " + requestedLocation
                    + ". Strict profile mode does not allow fallback requirements.");
        }

        return requestedLocation;
    }

    private boolean isMissingFileLocation(String location) {
        if (location == null || location.isBlank()) {
            return false;
        }
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return false;
        }
        return !Files.exists(Path.of(location));
    }

    private SourceType detectSourceType(String location) {
        return location.startsWith("http://") || location.startsWith("https://")
                ? SourceType.URL
                : SourceType.FILE;
    }
}
