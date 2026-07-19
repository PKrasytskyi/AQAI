package ua.demo.agentlab.demo;

import ua.demo.agentlab.ui.capability.LogoutAccessMode;

import java.util.List;

/** Resolved, non-secret input identity written into demo-input-readiness.json. */
public record DemoInputResolution(
        String projectProfilePath,
        String requirementFixturePath,
        String profileId,
        String projectName,
        String baseUrl,
        String homeRoute,
        String loginRoute,
        String authenticatedRoute,
        LogoutAccessMode logoutAccessMode,
        List<String> lifecycle,
        List<String> expectedPomNames,
        List<String> expectedScenarioIds,
        List<String> expectedGeneratedTests
) {
    public DemoInputResolution {
        projectProfilePath = safe(projectProfilePath);
        requirementFixturePath = safe(requirementFixturePath);
        profileId = safe(profileId);
        projectName = safe(projectName);
        baseUrl = safe(baseUrl);
        homeRoute = safe(homeRoute);
        loginRoute = safe(loginRoute);
        authenticatedRoute = safe(authenticatedRoute);
        logoutAccessMode = logoutAccessMode == null ? LogoutAccessMode.UNKNOWN : logoutAccessMode;
        lifecycle = lifecycle == null ? List.of() : List.copyOf(lifecycle);
        expectedPomNames = expectedPomNames == null ? List.of() : List.copyOf(expectedPomNames);
        expectedScenarioIds = expectedScenarioIds == null ? List.of() : List.copyOf(expectedScenarioIds);
        expectedGeneratedTests = expectedGeneratedTests == null ? List.of() : List.copyOf(expectedGeneratedTests);
    }

    public static DemoInputResolution empty() {
        return new DemoInputResolution("", "", "", "", "", "", "", "",
                LogoutAccessMode.UNKNOWN, List.of(), List.of(), List.of(), List.of());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
