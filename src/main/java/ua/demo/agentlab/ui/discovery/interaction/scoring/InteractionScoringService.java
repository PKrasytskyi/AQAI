package ua.demo.agentlab.ui.discovery.interaction.scoring;

import ua.demo.agentlab.ui.discovery.interaction.model.InteractionCandidate;
import ua.demo.agentlab.ui.discovery.interaction.model.InteractionVerification;
import ua.demo.agentlab.ui.discovery.interaction.model.LiveVerifiedInteraction;
import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.interaction.model.ScoreBreakdown;

import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned and explainable scoring. No stage silently recalculates the score. */
public final class InteractionScoringService {

    private final InteractionScoringPolicy policy;

    public InteractionScoringService() {
        this(InteractionScoringPolicy.v1());
    }

    public InteractionScoringService(InteractionScoringPolicy policy) {
        this.policy = policy == null ? InteractionScoringPolicy.v1() : policy;
    }

    public InteractionCandidate scoreIntrinsic(InteractionCandidate candidate) {
        double uniqueness = candidate.uniqueOnPage() ? 1.0d : candidate.uniqueWithinComponent() ? 0.85d : 0.0d;
        double context = candidate.componentId().isBlank() ? 0.35d : 0.90d;
        double history = candidate.stableAcrossRuns() ? 1.0d : 0.45d;
        Map<String, Double> factors = ordered(
                "locatorStability", candidate.locatorStability(),
                "uniqueness", uniqueness,
                "actionCompatibility", candidate.actionCompatibility(),
                "componentContext", context,
                "historicalStability", history,
                "riskPenalty", riskPenalty(candidate));
        ScoreBreakdown breakdown = breakdown(factors, policy.intrinsicWeights(), "riskPenalty");
        return new InteractionCandidate(
                candidate.elementKey(), candidate.actionKey(), candidate.locatorEvidenceId(),
                candidate.sourceActionId(), candidate.sourceLocatorId(), candidate.pageId(),
                candidate.route(), candidate.componentId(), candidate.action(), candidate.strategy(), candidate.value(),
                candidate.visible(), candidate.enabled(), candidate.sameOrigin(), candidate.uniqueOnPage(),
                candidate.uniqueWithinComponent(), candidate.stableAcrossRuns(), candidate.globalMatchCount(),
                candidate.componentMatchCount(), candidate.locatorStability(), candidate.actionCompatibility(),
                candidate.risks(), candidate.provenance(), breakdown);
    }

    public RequirementScopedInteraction scoreScoped(
            InteractionCandidate candidate, java.util.Set<String> requirementIds,
            double requirementRelevance, double routeMatch, double ownership
    ) {
        InteractionCandidate intrinsic = candidate.intrinsicScore().schemaVersion().isBlank()
                ? scoreIntrinsic(candidate) : candidate;
        Map<String, Double> factors = ordered(
                "intrinsic", intrinsic.intrinsicScore().finalScore(),
                "requirementRelevance", requirementRelevance,
                "routeMatch", routeMatch,
                "ownership", ownership);
        return new RequirementScopedInteraction(intrinsic, requirementIds, requirementRelevance, routeMatch,
                ownership, breakdown(factors, policy.scopedWeights(), ""));
    }

    public LiveVerifiedInteraction scoreLive(
            RequirementScopedInteraction scoped, InteractionVerification verification
    ) {
        double live = average(verification.locatorVerified(), verification.actionVerified());
        double postcondition = average(verification.stateTransitionVerified(), verification.postconditionVerified());
        Map<String, Double> factors = ordered(
                "scoped", scoped.scopedScore().finalScore(),
                "liveVerification", live,
                "postcondition", postcondition);
        return new LiveVerifiedInteraction(scoped, verification, breakdown(factors, policy.liveWeights(), ""));
    }

    public String schemaVersion() {
        return policy.schemaVersion();
    }

    private ScoreBreakdown breakdown(Map<String, Double> factors, Map<String, Double> weights, String penaltyKey) {
        double score = weights.entrySet().stream()
                .mapToDouble(entry -> clamp(factors.getOrDefault(entry.getKey(), 0.0d)) * entry.getValue()).sum();
        if (!penaltyKey.isBlank()) score -= factors.getOrDefault(penaltyKey, 0.0d);
        return new ScoreBreakdown(policy.schemaVersion(), factors, clamp(score));
    }

    private double riskPenalty(InteractionCandidate candidate) {
        double penalty = candidate.risks().stream().map(String::toLowerCase).mapToDouble(risk ->
                risk.contains("external") || risk.contains("token") || risk.contains("absolute") ? 0.40d
                        : risk.contains("dynamic") || risk.contains("hidden") ? 0.25d : 0.04d).sum();
        return Math.min(0.80d, penalty);
    }

    private double average(boolean first, boolean second) {
        return ((first ? 1.0d : 0.0d) + (second ? 1.0d : 0.0d)) / 2.0d;
    }

    private Map<String, Double> ordered(Object... values) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), clamp((Double) values[index + 1]));
        }
        return result;
    }

    private double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d;
    }
}
