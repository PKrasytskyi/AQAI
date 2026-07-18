package ua.demo.agentlab.ui.discovery.interaction.verification;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;

public final class LocatorVerifier {

    public LocatorVerificationResult verify(RequirementScopedInteraction scoped, SpaLiveTargetedVerificationResult live) {
        String locatorId = scoped.candidate().sourceLocatorId();
        TargetedLocatorVerification match = live == null ? null : live.locatorVerifications().stream()
                .filter(item -> item.locatorId().equals(locatorId))
                .filter(item -> item.pageId().equals(scoped.candidate().pageId()))
                .findFirst().orElse(null);
        if (match == null) {
            return new LocatorVerificationResult(null,
                    VerificationSignal.failed("live locator verification is missing", "locator-verifier"));
        }
        boolean unique = match.verified() && scoped.candidate().componentMatchCount() == 1;
        return new LocatorVerificationResult(match, new VerificationSignal(
                match.verified() && unique,
                match.verified() && unique ? match.qualityScore() : 0.0d,
                unique ? match.reason() : "locator is not unique in its component",
                java.util.List.of("locator-verifier", "locator-id:" + locatorId)));
    }

    public record LocatorVerificationResult(TargetedLocatorVerification evidence, VerificationSignal signal) { }
}
