package ua.demo.agentlab.ui.discovery.interaction.scoring;

import java.util.Map;

public record InteractionScoringPolicy(
        String schemaVersion,
        Map<String, Double> intrinsicWeights,
        Map<String, Double> scopedWeights,
        Map<String, Double> liveWeights
) {
    public static final String SCHEMA_VERSION = "interaction-score.v1";

    public InteractionScoringPolicy {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        intrinsicWeights = Map.copyOf(intrinsicWeights);
        scopedWeights = Map.copyOf(scopedWeights);
        liveWeights = Map.copyOf(liveWeights);
        requireUnitWeight(intrinsicWeights, "intrinsic");
        requireUnitWeight(scopedWeights, "scoped");
        requireUnitWeight(liveWeights, "live");
    }

    public static InteractionScoringPolicy v1() {
        return new InteractionScoringPolicy(
                SCHEMA_VERSION,
                Map.of("locatorStability", 0.35d, "uniqueness", 0.25d,
                        "actionCompatibility", 0.25d, "componentContext", 0.10d,
                        "historicalStability", 0.05d),
                Map.of("intrinsic", 0.70d, "requirementRelevance", 0.15d,
                        "routeMatch", 0.08d, "ownership", 0.07d),
                Map.of("scoped", 0.55d, "liveVerification", 0.30d, "postcondition", 0.15d));
    }

    private static void requireUnitWeight(Map<String, Double> weights, String stage) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - 1.0d) > 0.0001d) {
            throw new IllegalArgumentException(stage + " scoring weights must equal 1.0, got " + total);
        }
    }
}
