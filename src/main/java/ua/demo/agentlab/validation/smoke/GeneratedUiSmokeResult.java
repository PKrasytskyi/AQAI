package ua.demo.agentlab.validation.smoke;

import java.util.List;

public record GeneratedUiSmokeResult(
        GeneratedUiSmokeStatus status,
        String summary,
        int filesChecked,
        List<GeneratedUiSmokeIssue> issues
) {
    public GeneratedUiSmokeResult {
        status = status == null ? GeneratedUiSmokeStatus.SKIPPED : status;
        summary = summary == null ? "" : summary.trim();
        filesChecked = Math.max(0, filesChecked);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean passed() {
        return status == GeneratedUiSmokeStatus.PASSED;
    }
}
