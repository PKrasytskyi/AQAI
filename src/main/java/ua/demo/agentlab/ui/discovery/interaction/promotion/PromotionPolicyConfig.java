package ua.demo.agentlab.ui.discovery.interaction.promotion;

public record PromotionPolicyConfig(double confirmedScore, double liveVerifiedScore, int maxAlternatives) {
    public static final String SCHEMA_VERSION = "interaction-promotion.v1";

    public PromotionPolicyConfig {
        confirmedScore = clamp(confirmedScore);
        liveVerifiedScore = clamp(liveVerifiedScore);
        maxAlternatives = Math.max(1, Math.min(3, maxAlternatives));
        if (confirmedScore < liveVerifiedScore) {
            throw new IllegalArgumentException("confirmed score cannot be below live-verified score");
        }
    }

    public static PromotionPolicyConfig defaults() {
        return new PromotionPolicyConfig(0.80d, 0.65d, 3);
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d;
    }
}
