package ua.demo.agentlab.artifactreuse.planner;

import java.util.List;

public record ReusePlannerResult(
        String schemaVersion,
        boolean enabled,
        List<RequirementReusePlan> plans,
        int stableReuseCount,
        int discoveryCount,
        int needsReviewCount,
        String vectorUnavailableReason
) {
    public ReusePlannerResult {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? "reuse-planner.v1" : schemaVersion.trim();
        plans = plans == null ? List.of() : List.copyOf(plans);
        stableReuseCount = Math.max(0, stableReuseCount);
        discoveryCount = Math.max(0, discoveryCount);
        needsReviewCount = Math.max(0, needsReviewCount);
        vectorUnavailableReason = vectorUnavailableReason == null ? "" : vectorUnavailableReason.trim();
    }
}
