package ua.demo.agentlab.validation.smoke;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

public record LiveSmokePlan(
        ProjectProfile profile,
        GeneratedSourceFile sourcePage,
        GeneratedSourceFile targetPage,
        String sourceRoute,
        String targetRoute,
        String postActionRoute
) {
    public LiveSmokePlan {
        sourceRoute = clean(sourceRoute);
        targetRoute = clean(targetRoute);
        postActionRoute = clean(postActionRoute);
    }

    public boolean hasSourcePage() {
        return sourcePage != null;
    }

    public boolean hasTargetPage() {
        return targetPage != null;
    }

    public boolean hasTargetRoute() {
        return !targetRoute.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
