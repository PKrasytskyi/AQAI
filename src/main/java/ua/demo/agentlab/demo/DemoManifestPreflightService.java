package ua.demo.agentlab.demo;

import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.RuntimeProperties;
import ua.demo.agentlab.ui.capability.LogoutAccessMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DemoManifestPreflightService {

    private static final Pattern REQUIREMENT_ID = Pattern.compile("(?im)^## Requirement:\\s*([A-Za-z0-9_-]+)");
    private static final Pattern REQUIREMENT_CAPABILITY = Pattern.compile(
            "(?im)^### Capability\\s*\\R+\\s*`?([A-Za-z0-9_-]+)`?");
    private static final Pattern LOGOUT_ACCESS_MODE = Pattern.compile(
            "(?im)logoutAccessMode\\s*:\\s*([A-Za-z0-9_-]+)");
    private static final Set<String> FORBIDDEN_DEMO_CAPABILITIES = Set.of(
            "MODULE_NAVIGATION", "RECORD_LIST", "RECORD_DETAILS", "FILTER", "SEARCH",
            "RESULTS_COLLECTION", "CART", "CATALOG", "CHECKOUT", "PRODUCT"
    );

    public DemoPreflightReport validate(DemoManifest manifest, Path workspaceRoot) {
        return validate(manifest, workspaceRoot, name -> {
            String systemValue = System.getProperty(name);
            return systemValue == null || systemValue.isBlank() ? System.getenv(name) : systemValue;
        });
    }

    public DemoPreflightReport validate(
            DemoManifest manifest,
            Path workspaceRoot,
            Function<String, String> environment
    ) {
        if (manifest == null) {
            return report("", DemoInputResolution.empty(),
                    List.of(issue("MANIFEST_MISSING", "Demo manifest is required.")));
        }
        Path root = workspaceRoot == null ? Path.of("").toAbsolutePath() : workspaceRoot.toAbsolutePath().normalize();
        List<DemoPreflightIssue> issues = new ArrayList<>();
        if (!DemoManifest.SCHEMA_VERSION.equals(manifest.schemaVersion())) {
            issues.add(issue("SCHEMA_VERSION_INVALID", "Expected " + DemoManifest.SCHEMA_VERSION
                    + " but found " + manifest.schemaVersion() + "."));
        }

        Path profilePath = resolve(root, manifest.projectProfilePath());
        Path requirementPath = resolve(root, manifest.requirementFixturePath());
        if (!Files.isRegularFile(profilePath)) {
            issues.add(issue("PROJECT_PROFILE_MISSING", "Project profile does not exist: " + profilePath));
        }
        if (!Files.isRegularFile(requirementPath)) {
            issues.add(issue("REQUIREMENT_FIXTURE_MISSING", "Requirement fixture does not exist: " + requirementPath));
        }

        ProjectProfile profile = Files.isRegularFile(profilePath)
                ? validateProfile(manifest, root, profilePath, issues)
                : null;
        if (Files.isRegularFile(requirementPath)) {
            validateRequirements(manifest, requirementPath, issues);
        }
        validateExpectedContract(manifest, issues);
        validateEnvironment(manifest, environment == null ? ignored -> null : environment, issues);
        return report(manifest.demoId(), resolution(manifest, profile), issues);
    }

    private ProjectProfile validateProfile(
            DemoManifest manifest,
            Path workspaceRoot,
            Path profilePath,
            List<DemoPreflightIssue> issues
    ) {
        Properties values = new Properties();
        values.setProperty("project.profile.file", profilePath.toString());
        RuntimeProperties runtime = new RuntimeProperties(values);
        PropertiesProjectProfileLoader loader = new PropertiesProjectProfileLoader(runtime);
        ProjectProfile profile;
        try {
            profile = loader.loadDefaultProfile();
        } catch (RuntimeException exception) {
            issues.add(issue("PROJECT_PROFILE_INVALID", exception.getMessage()));
            return null;
        }
        if (profile.homeRoute().isBlank() || profile.loginRoute().isBlank() || profile.authenticatedRoute().isBlank()) {
            issues.add(issue("EXPLICIT_ROUTES_REQUIRED",
                    "Demo profile must define explicit home, login, and authenticated routes."));
        }
        String configuredRequirement = normalizePath(loader.defaultRequirementLocation());
        String expectedRequirement = normalizePath(workspaceRoot.relativize(
                resolve(workspaceRoot, manifest.requirementFixturePath())).toString());
        if (!configuredRequirement.equals(expectedRequirement)) {
            issues.add(issue("PROFILE_REQUIREMENT_MISMATCH", "Profile requirement file '" + configuredRequirement
                    + "' does not match manifest fixture '" + expectedRequirement + "'."));
        }
        if (!routeMatches(profile.loginRoute(), manifest.expectedFinalRoute())) {
            issues.add(issue("FINAL_ROUTE_MISMATCH", "Expected final route must match the explicit login route."));
        }
        return profile;
    }

    private void validateRequirements(
            DemoManifest manifest,
            Path requirementPath,
            List<DemoPreflightIssue> issues
    ) {
        try {
            String markdown = Files.readString(requirementPath, StandardCharsets.UTF_8);
            Set<String> requirementIds = matches(markdown, REQUIREMENT_ID);
            Set<String> capabilities = matches(markdown, REQUIREMENT_CAPABILITY);
            Set<String> logoutModes = matches(markdown, LOGOUT_ACCESS_MODE);
            if (!requirementIds.equals(new LinkedHashSet<>(manifest.expectedScenarioIds()))) {
                issues.add(issue("SCENARIO_IDS_MISMATCH", "Requirement IDs " + requirementIds
                        + " do not match manifest scenario IDs " + manifest.expectedScenarioIds() + "."));
            }
            Set<String> forbidden = capabilities.stream()
                    .map(value -> value.toUpperCase(Locale.ROOT))
                    .filter(FORBIDDEN_DEMO_CAPABILITIES::contains)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!forbidden.isEmpty()) {
                issues.add(issue("UNRELATED_CAPABILITY", "Authentication demo contains unrelated capabilities: " + forbidden));
            }
            if (!capabilities.contains("AUTHENTICATION") || !capabilities.contains("LOGOUT")) {
                issues.add(issue("REQUIRED_CAPABILITY_MISSING",
                        "Authentication demo must declare AUTHENTICATION and LOGOUT requirements."));
            }
            Set<String> expectedModes = Set.of(manifest.expectedLogoutAccessMode().name());
            if (!logoutModes.equals(expectedModes)) {
                issues.add(issue("LOGOUT_ACCESS_MODE_MISMATCH", "Requirement logout access modes " + logoutModes
                        + " do not match manifest mode " + manifest.expectedLogoutAccessMode() + "."));
            }
        } catch (IOException exception) {
            issues.add(issue("REQUIREMENT_FIXTURE_UNREADABLE", exception.getMessage()));
        }
    }

    private void validateExpectedContract(DemoManifest manifest, List<DemoPreflightIssue> issues) {
        Set<String> expectedCapabilities = manifest.expectedPageCapabilities().stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (!expectedCapabilities.containsAll(Set.of("AUTHENTICATION", "AUTHENTICATED_AREA", "LOGOUT"))) {
            issues.add(issue("EXPECTED_CAPABILITIES_INCOMPLETE",
                    "Authentication demo must expect AUTHENTICATION, AUTHENTICATED_AREA, and LOGOUT."));
        }
        LogoutAccessMode accessMode = manifest.expectedLogoutAccessMode();
        if (accessMode == LogoutAccessMode.UNKNOWN) {
            issues.add(issue("LOGOUT_ACCESS_MODE_MISSING", "Demo manifest must declare a logout access mode."));
        } else {
            String requiredTopology = accessMode == LogoutAccessMode.USER_MENU
                    ? "USER_MENU"
                    : "DIRECT_LOGOUT_CONTROL";
            String forbiddenTopology = accessMode == LogoutAccessMode.USER_MENU
                    ? "DIRECT_LOGOUT_CONTROL"
                    : "USER_MENU";
            if (!expectedCapabilities.contains(requiredTopology) || expectedCapabilities.contains(forbiddenTopology)) {
                issues.add(issue("LOGOUT_TOPOLOGY_INVALID", "Expected capabilities must contain " + requiredTopology
                        + " and must not contain " + forbiddenTopology + "."));
            }
            if (!manifest.expectedLifecycle().equals(accessMode.lifecycle())) {
                issues.add(issue("LIFECYCLE_MISMATCH", "Expected lifecycle " + manifest.expectedLifecycle()
                        + " does not match " + accessMode.lifecycle() + "."));
            }
        }
        Set<String> pomNames = new LinkedHashSet<>(manifest.expectedPomNames());
        if (pomNames.size() != 2 || pomNames.stream().anyMatch(name -> !name.endsWith("Page"))) {
            issues.add(issue("EXPECTED_POMS_INVALID",
                    "Authentication demo must declare exactly two distinct Page Object names."));
        }
        Set<String> generatedTests = new LinkedHashSet<>(manifest.expectedGeneratedTests());
        if (generatedTests.size() != manifest.expectedScenarioIds().size()
                || generatedTests.stream().anyMatch(name -> !name.endsWith("Test"))) {
            issues.add(issue("EXPECTED_TESTS_INVALID",
                    "Demo must declare one distinct generated TestNG class per expected scenario."));
        }
        Set<String> requiredStages = Set.of(
                "REQUIREMENTS", "NORMALIZED_REQUIREMENTS", "CANONICAL_TEST_CASES", "CONFIRMED_UI_CATALOG",
                "POM_CONTRACTS", "PAGE_OBJECTS", "UI_TEST_CONTRACTS", "GENERATED_TESTNG_TESTS",
                "COMPILE", "EXECUTION", "QUALITY_REPORT"
        );
        Set<String> declaredStages = manifest.expectedPipelineStages().stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (!declaredStages.containsAll(requiredStages)) {
            issues.add(issue("EXPECTED_PIPELINE_INCOMPLETE",
                    "Demo manifest must declare the complete requirements-to-execution pipeline."));
        }
        if (!manifest.generatedTestExecutionRequired()) {
            issues.add(issue("GENERATED_TEST_EXECUTION_NOT_REQUIRED",
                    "Build Week demo must require execution of manifest-owned generated tests."));
        }
        for (String requiredSchema : List.of(
                "requirements",
                "structuredBehaviorContracts",
                "requirementGovernance",
                "canonicalTestCase",
                "goldenRequirementSnapshot",
                "pomContract",
                "uiTestContract",
                "generatedSourceManifest",
                "generatedTestExecution"
        )) {
            if (manifest.schemaVersions().getOrDefault(requiredSchema, "").isBlank()) {
                issues.add(issue("SCHEMA_VERSION_MISSING", "Missing schema version for " + requiredSchema + "."));
            }
        }
    }

    private void validateEnvironment(
            DemoManifest manifest,
            Function<String, String> environment,
            List<DemoPreflightIssue> issues
    ) {
        List<String> missing = manifest.requiredEnvironmentVariables().stream()
                .filter(name -> {
                    String value = environment.apply(name);
                    return value == null || value.isBlank();
                })
                .toList();
        if (!missing.isEmpty()) {
            issues.add(issue("MISSING_REQUIRED_ENVIRONMENT",
                    "Missing required environment variables: " + String.join(", ", missing) + "."));
        }
        String dbStatus = environment.apply("KNOWLEDGE_DB_STATUS");
        if (dbStatus == null || dbStatus.isBlank()) {
            return;
        }
        String normalizedStatus = dbStatus.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("true", "false").contains(normalizedStatus)) {
            issues.add(issue("DATABASE_STATUS_INVALID",
                    "KNOWLEDGE_DB_STATUS must be either true or false."));
            return;
        }
        boolean databaseEnabled = Boolean.parseBoolean(normalizedStatus);
        if (manifest.databaseMode() == DemoDatabaseMode.WITHOUT_DB_BASELINE && databaseEnabled) {
            issues.add(issue("DATABASE_MODE_MISMATCH",
                    "Demo manifest requires KNOWLEDGE_DB_STATUS=false for the without-DB baseline run."));
        } else if (manifest.databaseMode() == DemoDatabaseMode.WITH_DB_REQUIRED && !databaseEnabled) {
            issues.add(issue("DATABASE_MODE_MISMATCH",
                    "Demo manifest requires KNOWLEDGE_DB_STATUS=true for the with-DB run."));
        } else if (manifest.databaseMode() == DemoDatabaseMode.UNKNOWN) {
            issues.add(issue("DATABASE_MODE_INVALID",
                    "Demo manifest databaseMode must be without-db-baseline, with-db-required, or environment-controlled."));
        }
    }

    private Set<String> matches(String text, Pattern pattern) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        while (matcher.find()) {
            result.add(matcher.group(1).trim().toUpperCase(Locale.ROOT));
        }
        return result;
    }

    private DemoPreflightReport report(
            String demoId,
            DemoInputResolution resolution,
            List<DemoPreflightIssue> issues
    ) {
        return new DemoPreflightReport(
                DemoPreflightReport.SCHEMA_VERSION,
                demoId,
                issues.isEmpty(),
                resolution,
                issues
        );
    }

    private DemoInputResolution resolution(DemoManifest manifest, ProjectProfile profile) {
        return new DemoInputResolution(
                manifest.projectProfilePath(),
                manifest.requirementFixturePath(),
                profile == null ? "" : profile.profileId(),
                profile == null ? "" : profile.projectName(),
                profile == null ? "" : profile.baseUrl(),
                profile == null ? "" : profile.homeRoute(),
                profile == null ? "" : profile.loginRoute(),
                profile == null ? "" : profile.authenticatedRoute(),
                manifest.expectedLogoutAccessMode(),
                manifest.expectedLifecycle(),
                manifest.expectedPomNames(),
                manifest.expectedScenarioIds(),
                manifest.expectedGeneratedTests()
        );
    }

    private DemoPreflightIssue issue(String code, String message) {
        return new DemoPreflightIssue(code, message == null || message.isBlank() ? code : message);
    }

    private Path resolve(Path root, String configuredPath) {
        Path path = Path.of(configuredPath == null ? "" : configuredPath);
        return (path.isAbsolute() ? path : root.resolve(path)).normalize();
    }

    private boolean routeMatches(String left, String right) {
        return normalizeRoute(left).equals(normalizeRoute(right));
    }

    private String normalizeRoute(String value) {
        String route = value == null ? "" : value.trim();
        if (route.length() > 1 && route.endsWith("/")) {
            route = route.substring(0, route.length() - 1);
        }
        return route;
    }

    private String normalizePath(String value) {
        return (value == null ? "" : value.trim()).replace('\\', '/');
    }
}
