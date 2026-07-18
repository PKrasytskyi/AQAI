package ua.demo.agentlab.ui.discovery.interaction.verification;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;

public final class ActionVerifier {

    public ActionVerificationResult verify(RequirementScopedInteraction scoped, SpaLiveTargetedVerificationResult live) {
        String actionId = scoped.candidate().sourceActionId();
        TargetedActionVerification match = live == null ? null : live.actionVerifications().stream()
                .filter(item -> item.actionId().equals(actionId))
                .filter(item -> item.pageId().equals(scoped.candidate().pageId()))
                .filter(item -> item.intent().equalsIgnoreCase(scoped.candidate().action().name()))
                .findFirst().orElse(null);
        if (match == null) {
            return new ActionVerificationResult(null,
                    VerificationSignal.failed("live action verification is missing", "action-verifier"));
        }
        return new ActionVerificationResult(match, new VerificationSignal(
                match.verified(), match.verified() ? match.confidence() : 0.0d, match.reason(),
                java.util.List.of("action-verifier", "action-id:" + match.actionId())));
    }

    public record ActionVerificationResult(TargetedActionVerification evidence, VerificationSignal signal) { }
}
