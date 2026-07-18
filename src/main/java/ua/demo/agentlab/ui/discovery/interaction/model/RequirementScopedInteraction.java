package ua.demo.agentlab.ui.discovery.interaction.model;

import java.util.Set;

public record RequirementScopedInteraction(
        InteractionCandidate candidate,
        Set<String> requirementIds,
        double requirementRelevance,
        double routeMatch,
        double ownershipScore,
        ScoreBreakdown scopedScore
) {
    public RequirementScopedInteraction {
        if (candidate == null) throw new IllegalArgumentException("Scoped interaction requires a candidate");
        requirementIds = requirementIds == null ? Set.of() : Set.copyOf(requirementIds);
        if (requirementIds.isEmpty()) throw new IllegalArgumentException("Scoped interaction requires requirement ownership");
        requirementRelevance = clamp(requirementRelevance);
        routeMatch = clamp(routeMatch);
        ownershipScore = clamp(ownershipScore);
        scopedScore = scopedScore == null ? candidate.intrinsicScore() : scopedScore;
    }

    private static double clamp(double value) { return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d; }
}
