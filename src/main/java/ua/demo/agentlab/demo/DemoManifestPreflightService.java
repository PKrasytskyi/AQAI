package ua.demo.agentlab.demo;

import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.RuntimeProperties;

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
            return report("", List.of(issue("MANIFEST_MISSING", "Demo manifest is required.")));
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

        if (Files.isRegularFile(profilePath)) {
            validateProfile(manifest, root, profilePath, issues);
        }
        if (Files.isRegularFile(requirementPath)) {
            validateRequirements(manifest, requirementPath, issues);
        }
        validateExpectedContract(manifest, issues);
        validateEnvironment(manifest, environment == null ? ignored -> null : environment, issues);
        return report(manifest.demoId(), issues);
    }

    private void validateProfile(
            DemoManifest manifest,
            Path workspaceRoot,
            Path profilePath,
            List<DemoPreflightIssue> issues
    ) {
        Properties values = new Properties();
        values.setProperty("project.profile.file", profilePath.toString());
        RuntimeProperties runtime = new RuntimeProperties(values);
        PropertiesProjectProfileLoader loader = new PropertiesProjectProfileLoader(runtime);
        ProjectProfile profile = loader.loadDefaultProfile();
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
        if (!routeMatches(profile.loginRoute(), profile.homeRoute())) {
            issues.add(issue("HOME_ROUTE_MISMATCH",
                    "The authentication demo expects home and login to resolve to the same explicit route."));
        }
        if (!routeMatches(profile.loginRoute(), manifest.expectedFinalRoute())) {
            issues.add(issue("FINAL_ROUTE_MISMATCH", "Expected final route must match the explicit login route."));
        }
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
        } catch (IOException exception) {
            issues.add(issue("REQUIREMENT_FIXTURE_UNREADABLE", exception.getMessage()));
        }
    }

    private void validateExpectedContract(DemoManifest manifest, List<DemoPreflightIssue> issues) {
        Set<String> expectedCapabilities = manifest.expectedPageCapabilities().stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (!expectedCapabilities.containsAll(Set.of("AUTHENTICATION", "AUTHENTICATED_AREA", "USER_MENU", "LOGOUT"))) {
            issues.add(issue("EXPECTED_CAPABILITIES_INCOMPLETE",
                    "OrangeHRM demo must expect AUTHENTICATION, AUTHENTICATED_AREA, USER_MENU, and LOGOUT."));
        }
        if (!new LinkedHashSet<>(manifest.expectedPomNames()).equals(Set.of("LoginPage", "DashboardPage"))) {
            issues.add(issue("EXPECTED_POMS_INVALID", "OrangeHRM demo must expect LoginPage and DashboardPage only."));
        }
        for (String requiredSchema : List.of("requirements", "canonicalTestCase", "pomContract")) {
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
        if (dbStatus != null && !dbStatus.isBlank()
                && manifest.databaseMode().equalsIgnoreCase("without-db-baseline")
                && Boolean.parseBoolean(dbStatus.trim())) {
            issues.add(issue("DATABASE_MODE_MISMATCH",
                    "Demo manifest requires KNOWLEDGE_DB_STATUS=false for the without-DB baseline run."));
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

    private DemoPreflightReport report(String demoId, List<DemoPreflightIssue> issues) {
        return new DemoPreflightReport(DemoPreflightReport.SCHEMA_VERSION, demoId, issues.isEmpty(), issues);
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
