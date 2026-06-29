package ua.demo.agentlab.ai.artifactdiff;

import java.util.List;

public record ArtifactDiffReport(
        String currentRunId,
        String previousRunId,
        List<ArtifactCategoryDiff> categories,
        List<ArtifactRegression> regressions
) {
    public ArtifactDiffReport {
        currentRunId = safe(currentRunId);
        previousRunId = safe(previousRunId);
        categories = categories == null ? List.of() : List.copyOf(categories);
        regressions = regressions == null ? List.of() : List.copyOf(regressions);
    }

    public boolean hasRegressions() {
        return !regressions.isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
