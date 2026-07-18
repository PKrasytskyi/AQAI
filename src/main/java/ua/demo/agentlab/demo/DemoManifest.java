package ua.demo.agentlab.demo;

import ua.demo.agentlab.ui.capability.LogoutAccessMode;

import java.util.List;
import java.util.Map;

public record DemoManifest(
        String schemaVersion,
        String demoId,
        String projectProfilePath,
        String requirementFixturePath,
        List<String> requiredEnvironmentVariables,
        List<String> expectedPageCapabilities,
        List<String> expectedScenarioIds,
        List<String> expectedPomNames,
        LogoutAccessMode expectedLogoutAccessMode,
        List<String> expectedLifecycle,
        String expectedFinalRoute,
        String expectedFinalState,
        DemoDatabaseMode databaseMode,
        String aiMode,
        Map<String, String> schemaVersions
) {
    public static final String SCHEMA_VERSION = "demo-manifest.v2";

    public DemoManifest {
        schemaVersion = safe(schemaVersion);
        demoId = safe(demoId);
        projectProfilePath = safe(projectProfilePath);
        requirementFixturePath = safe(requirementFixturePath);
        requiredEnvironmentVariables = copy(requiredEnvironmentVariables);
        expectedPageCapabilities = copy(expectedPageCapabilities);
        expectedScenarioIds = copy(expectedScenarioIds);
        expectedPomNames = copy(expectedPomNames);
        expectedLogoutAccessMode = expectedLogoutAccessMode == null ? LogoutAccessMode.UNKNOWN : expectedLogoutAccessMode;
        expectedLifecycle = copy(expectedLifecycle);
        expectedFinalRoute = safe(expectedFinalRoute);
        expectedFinalState = safe(expectedFinalState);
        databaseMode = databaseMode == null ? DemoDatabaseMode.UNKNOWN : databaseMode;
        aiMode = safe(aiMode);
        schemaVersions = schemaVersions == null ? Map.of() : Map.copyOf(schemaVersions);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : values.stream()
                .map(DemoManifest::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
