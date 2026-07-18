package ua.demo.agentlab.ui.discovery.interaction.promotion;

import ua.demo.agentlab.ui.discovery.interaction.model.*;
import ua.demo.agentlab.ui.discovery.interaction.selection.InteractionSafetyDecision;
import ua.demo.agentlab.ui.discovery.interaction.selection.InteractionSafetyGate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The only policy assigning canonical interaction lifecycle and primary/standby ranks. */
public final class PromotionPolicy {

    private final PromotionPolicyConfig config;
    private final InteractionSafetyGate safetyGate;

    public PromotionPolicy() {
        this(PromotionPolicyConfig.defaults(), new InteractionSafetyGate());
    }

    public PromotionPolicy(PromotionPolicyConfig config, InteractionSafetyGate safetyGate) {
        this.config = config == null ? PromotionPolicyConfig.defaults() : config;
        this.safetyGate = safetyGate == null ? new InteractionSafetyGate() : safetyGate;
    }

    public List<LocatorPromotionDecision> decide(
            List<LiveVerifiedInteraction> verified,
            List<InteractionSafetyDecision> rejected
    ) {
        List<LocatorPromotionDecision> result = new ArrayList<>();
        if (rejected != null) {
            rejected.forEach(item -> result.add(new LocatorPromotionDecision(
                    rejected(item.interaction(), item),
                    InteractionEvidenceStatus.REJECTED, false, false, item.rejectionCodes())));
        }
        Map<String, List<LiveVerifiedInteraction>> groups = new LinkedHashMap<>();
        if (verified != null) verified.forEach(item -> groups.computeIfAbsent(
                item.scopedInteraction().candidate().actionKey().value(), ignored -> new ArrayList<>()).add(item));
        groups.values().forEach(group -> addRanked(group, result));
        return List.copyOf(result);
    }

    private void addRanked(List<LiveVerifiedInteraction> group, List<LocatorPromotionDecision> result) {
        List<LiveVerifiedInteraction> eligible = group.stream()
                .filter(this::livePassed)
                .sorted(Comparator.comparingDouble((LiveVerifiedInteraction item) -> item.finalScore().finalScore()).reversed())
                .limit(config.maxAlternatives())
                .toList();
        LiveVerifiedInteraction primary = eligible.isEmpty() ? null : eligible.get(0);
        LiveVerifiedInteraction standby = standby(eligible, primary);
        for (LiveVerifiedInteraction item : group) {
            InteractionEvidenceStatus status = status(item, PromotionHistory.empty());
            List<String> codes = decisionCodes(item, status);
            result.add(new LocatorPromotionDecision(item, status, item == primary, item == standby, codes));
        }
    }

    public InteractionEvidenceStatus status(LiveVerifiedInteraction item, PromotionHistory history) {
        if (!livePassed(item)) return InteractionEvidenceStatus.CANDIDATE;
        if (history != null && history.failures() >= 2 && history.flakyRate() > 0.10d) {
            return InteractionEvidenceStatus.DEGRADED;
        }
        double score = item.finalScore().finalScore();
        if (item.scopedInteraction().candidate().stableAcrossRuns() && score >= config.confirmedScore()) {
            return InteractionEvidenceStatus.CONFIRMED;
        }
        return score >= config.liveVerifiedScore()
                ? InteractionEvidenceStatus.LIVE_VERIFIED : InteractionEvidenceStatus.CANDIDATE;
    }

    private List<String> decisionCodes(LiveVerifiedInteraction item, InteractionEvidenceStatus status) {
        List<String> codes = new ArrayList<>();
        codes.add("POLICY_VERSION:" + PromotionPolicyConfig.SCHEMA_VERSION);
        codes.add("FINAL_SCORE:" + String.format(java.util.Locale.ROOT, "%.3f", item.finalScore().finalScore()));
        codes.add(switch (status) {
            case CONFIRMED -> "LIVE_POSTCONDITION_AND_CROSS_RUN_STABLE";
            case LIVE_VERIFIED -> "LIVE_VERIFIED_AWAITING_CROSS_RUN_STABILITY";
            case DEGRADED -> "RUNTIME_HISTORY_DEGRADED";
            case REJECTED -> "SAFETY_REJECTED";
            default -> "LIVE_VERIFICATION_INCOMPLETE";
        });
        return List.copyOf(codes);
    }

    private LiveVerifiedInteraction standby(List<LiveVerifiedInteraction> eligible, LiveVerifiedInteraction primary) {
        if (primary == null || eligible.size() < 2) return null;
        String family = safetyGate.selectorFamily(primary.scopedInteraction());
        return eligible.stream().skip(1)
                .filter(item -> !safetyGate.selectorFamily(item.scopedInteraction()).equals(family))
                .findFirst().orElse(eligible.get(1));
    }

    private boolean livePassed(LiveVerifiedInteraction item) {
        return item.verification().locatorVerified()
                && item.verification().actionVerified()
                && item.verification().stateTransitionVerified()
                && item.verification().postconditionVerified();
    }

    private LiveVerifiedInteraction rejected(RequirementScopedInteraction scoped, InteractionSafetyDecision decision) {
        InteractionVerification verification = new InteractionVerification(false, false, false, false,
                0, 0, 0.0d, String.join(",", decision.rejectionCodes()), List.of("interaction-safety-gate"));
        return new LiveVerifiedInteraction(scoped, verification, scoped.scopedScore());
    }
}
