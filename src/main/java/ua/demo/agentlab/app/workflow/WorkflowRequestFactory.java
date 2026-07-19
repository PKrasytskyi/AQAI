package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.ProjectProfileLoader;
import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.demo.DemoManifest;
import ua.demo.agentlab.demo.DemoManifestLoader;
import ua.demo.agentlab.requirements.model.SourceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
        DemoInvocation demo = resolveDemoInvocation(args);
        if (demo != null) {
            return createDemoRequest(demo);
        }
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

    private WorkflowRequest createDemoRequest(DemoInvocation invocation) {
        DemoManifest manifest = new DemoManifestLoader().load(invocation.manifestPath());
        System.setProperty("project.profile.file", manifest.projectProfilePath());
        System.setProperty("demo.preflight.enabled", "true");
        System.setProperty("demo.execution.active", "true");
        System.setProperty("demo.manifest.file", invocation.manifestPath().toString());
        System.setProperty("generated.test.execution.enabled",
                String.valueOf(manifest.generatedTestExecutionRequired()));
        PropertiesProjectProfileLoader demoProfileLoader = new PropertiesProjectProfileLoader();
        ProjectProfile profile = demoProfileLoader.loadDefaultProfile();
        String requirements = manifest.requirementFixturePath();
        if (isMissingFileLocation(requirements)) {
            throw new IllegalArgumentException("Demo requirement file not found: " + requirements);
        }
        return new WorkflowRequest(requirements, detectSourceType(requirements), WorkflowMode.AI_PROMPT, profile);
    }

    private DemoInvocation resolveDemoInvocation(String[] args) {
        String[] values = args == null ? new String[0] : args;
        for (int index = 0; index < values.length; index++) {
            if (!"--demo".equalsIgnoreCase(values[index])) {
                continue;
            }
            if (index + 1 >= values.length || values[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("--demo requires a demo id");
            }
            return new DemoInvocation(resolveDemoManifest(values[index + 1]));
        }
        return null;
    }

    private Path resolveDemoManifest(String demoId) {
        String normalizedId = demoId == null ? "" : demoId.trim().toLowerCase();
        if (normalizedId.isBlank() || !normalizedId.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("Invalid demo id: " + demoId);
        }
        Path demoRoot = Path.of("demo").toAbsolutePath().normalize();
        if (!Files.isDirectory(demoRoot)) {
            throw new IllegalArgumentException("Demo directory does not exist: " + demoRoot);
        }
        try (var directories = Files.list(demoRoot)) {
            List<Path> candidates = directories
                    .filter(Files::isDirectory)
                    .map(path -> path.resolve("demo-manifest.yaml"))
                    .filter(Files::isRegularFile)
                    .filter(path -> matchesDemo(normalizedId, path))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            if (candidates.size() != 1) {
                throw new IllegalArgumentException("Demo id '" + demoId
                        + "' must resolve to exactly one manifest, found " + candidates.size());
            }
            return candidates.get(0);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve demo manifest for " + demoId, exception);
        }
    }

    private boolean matchesDemo(String demoId, Path manifestPath) {
        String directory = manifestPath.getParent().getFileName().toString().toLowerCase();
        if (directory.equals(demoId) || directory.startsWith(demoId + "-")) {
            return true;
        }
        String manifestId = new DemoManifestLoader().load(manifestPath).demoId().toLowerCase();
        return manifestId.equals(demoId) || manifestId.startsWith(demoId + "-");
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

    private record DemoInvocation(Path manifestPath) {
    }
}
