package ua.demo.agentlab.validation.smoke;

import java.util.List;

public record LiveUiSmokeResult(
        GeneratedUiSmokeStatus status,
        String summary,
        String baseUrl,
        List<LiveUiSmokeStep> steps,
        List<GeneratedUiSmokeIssue> issues
) {
    public LiveUiSmokeResult {
        status = status == null ? GeneratedUiSmokeStatus.SKIPPED : status;
        summary = summary == null ? "" : summary.trim();
        baseUrl = baseUrl == null ? "" : baseUrl.trim();
        steps = steps == null ? List.of() : List.copyOf(steps);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean failed() {
        return status == GeneratedUiSmokeStatus.FAILED;
    }
}
