package ua.demo.agentlab.ui.discovery.interaction.model;

public record LiveVerifiedInteraction(
        RequirementScopedInteraction scopedInteraction,
        InteractionVerification verification,
        ScoreBreakdown finalScore
) {
    public LiveVerifiedInteraction {
        if (scopedInteraction == null || verification == null) {
            throw new IllegalArgumentException("Live verified interaction requires scoped evidence and verification");
        }
        finalScore = finalScore == null ? scopedInteraction.scopedScore() : finalScore;
    }
}
