package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.List;

/** Computes report readiness from terminal evidence flags only. */
public final class PageReadinessProjection {
    public Readiness project(UiEvidenceFunnelInput input, List<UiEvidenceRequirementResult> results) {
        boolean complete = results.stream().allMatch(result -> result.confirmedEvidencePath() || result.hasExplicitStop());
        boolean pomReady = !results.isEmpty()
                && results.stream().allMatch(UiEvidenceRequirementResult::confirmedEvidencePath);
        return new Readiness(complete, pomReady);
    }

    public record Readiness(boolean completenessPassed, boolean pomReadinessPassed) {}
}
