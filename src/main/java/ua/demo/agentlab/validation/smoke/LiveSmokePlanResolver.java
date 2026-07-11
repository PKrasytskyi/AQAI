package ua.demo.agentlab.validation.smoke;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.config.PropertiesProjectProfileLoader;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.Comparator;
import java.util.List;

public class LiveSmokePlanResolver {

    public LiveSmokePlan resolve(GeneratedUiSources sources) {
        ProjectProfile profile = new PropertiesProjectProfileLoader().loadDefaultProfile();
        return resolve(sources, profile);
    }

    LiveSmokePlan resolve(GeneratedUiSources sources, ProjectProfile profile) {
        GeneratedUiSources safeSources = sources == null ? new GeneratedUiSources(List.of(), List.of()) : sources;
        ProjectProfile safeProfile = profile;
        GeneratedSourceFile sourcePage = selectSourcePage(safeSources.pageObjectFiles(), safeProfile);
        GeneratedSourceFile targetPage = selectTargetPage(safeSources.pageObjectFiles(), safeProfile, sourcePage);
        return new LiveSmokePlan(
                safeProfile,
                sourcePage,
                targetPage,
                safeProfile == null ? "" : safeProfile.loginRoute(),
                safeProfile == null ? "" : safeProfile.authenticatedRoute(),
                safeProfile == null ? "" : safeProfile.loginRoute()
        );
    }

    private GeneratedSourceFile selectSourcePage(List<GeneratedSourceFile> pageObjects, ProjectProfile profile) {
        return safeFiles(pageObjects).stream()
                .max(Comparator.comparingInt(file -> sourceScore(file, profile)))
                .filter(file -> sourceScore(file, profile) > 0)
                .orElse(null);
    }

    private GeneratedSourceFile selectTargetPage(
            List<GeneratedSourceFile> pageObjects,
            ProjectProfile profile,
            GeneratedSourceFile sourcePage
    ) {
        return safeFiles(pageObjects).stream()
                .filter(file -> file != sourcePage)
                .max(Comparator.comparingInt(file -> targetScore(file, profile)))
                .filter(file -> targetScore(file, profile) > 0)
                .orElse(null);
    }

    private int sourceScore(GeneratedSourceFile file, ProjectProfile profile) {
        String content = normalize(file == null ? "" : file.content());
        String className = normalize(file == null ? "" : file.className());
        int score = 0;
        if (content.contains("usernameinput")) {
            score += 3;
        }
        if (content.contains("passwordinput")) {
            score += 3;
        }
        if (content.contains("loginbutton") || content.contains("submitbutton")) {
            score += 2;
        }
        if (content.contains("login(string username, string password)")) {
            score += 3;
        }
        if (containsRoute(content, profile == null ? "" : profile.loginRoute())) {
            score += 4;
        }
        if (className.contains("login") || className.contains("authentication")) {
            score += 1;
        }
        return score;
    }

    private int targetScore(GeneratedSourceFile file, ProjectProfile profile) {
        String content = normalize(file == null ? "" : file.content());
        String className = normalize(file == null ? "" : file.className());
        int score = 0;
        if (containsRoute(content, profile == null ? "" : profile.authenticatedRoute())) {
            score += 4;
        }
        if (content.contains("logoutlink") || content.contains("logout")) {
            score += 3;
        }
        if (content.contains("usermenutrigger")) {
            score += 2;
        }
        if (className.contains("dashboard") || className.contains("secure") || className.contains("authenticated")) {
            score += 1;
        }
        return score;
    }

    private boolean containsRoute(String content, String route) {
        String normalizedRoute = normalize(route);
        return !normalizedRoute.isBlank() && content.contains(normalizedRoute);
    }

    private List<GeneratedSourceFile> safeFiles(List<GeneratedSourceFile> files) {
        return files == null ? List.of() : files.stream().filter(file -> file != null).toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
