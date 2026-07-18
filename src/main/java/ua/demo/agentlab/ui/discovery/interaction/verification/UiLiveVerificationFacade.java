package ua.demo.agentlab.ui.discovery.interaction.verification;

import ua.demo.agentlab.ui.discovery.interaction.model.InteractionVerification;
import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;

import java.util.ArrayList;
import java.util.List;

/** One orchestration facade; individual verifiers own locator, action, transition, and postcondition rules. */
public final class UiLiveVerificationFacade {

    private final LocatorVerifier locatorVerifier;
    private final ActionVerifier actionVerifier;
    private final StateTransitionVerifier transitionVerifier;
    private final PostconditionVerifier postconditionVerifier;

    public UiLiveVerificationFacade() {
        this(new LocatorVerifier(), new ActionVerifier(), new StateTransitionVerifier(), new PostconditionVerifier());
    }

    public UiLiveVerificationFacade(
            LocatorVerifier locatorVerifier,
            ActionVerifier actionVerifier,
            StateTransitionVerifier transitionVerifier,
            PostconditionVerifier postconditionVerifier
    ) {
        this.locatorVerifier = java.util.Objects.requireNonNull(locatorVerifier);
        this.actionVerifier = java.util.Objects.requireNonNull(actionVerifier);
        this.transitionVerifier = java.util.Objects.requireNonNull(transitionVerifier);
        this.postconditionVerifier = java.util.Objects.requireNonNull(postconditionVerifier);
    }

    public InteractionVerification verify(
            RequirementScopedInteraction scoped,
            SpaLiveTargetedVerificationResult live
    ) {
        var locator = locatorVerifier.verify(scoped, live);
        var action = actionVerifier.verify(scoped, live);
        var transition = transitionVerifier.verify(scoped, live);
        boolean transitionRequired = transitionVerifier.requiresTransition(scoped.candidate().action());
        var postcondition = postconditionVerifier.verify(scoped, action.signal(), transition, transitionRequired);
        List<String> provenance = new ArrayList<>();
        provenance.addAll(locator.signal().provenance());
        provenance.addAll(action.signal().provenance());
        provenance.addAll(transition.provenance());
        provenance.addAll(postcondition.provenance());
        double confidence = java.util.stream.Stream.of(locator.signal(), action.signal(), transition, postcondition)
                .filter(VerificationSignal::passed)
                .mapToDouble(VerificationSignal::confidence).min().orElse(0.0d);
        return new InteractionVerification(
                locator.signal().passed(), action.signal().passed(), transition.passed(), postcondition.passed(),
                locator.evidence() == null ? 0 : scoped.candidate().globalMatchCount(),
                locator.evidence() == null ? 0 : scoped.candidate().componentMatchCount(),
                confidence, firstFailure(locator.signal(), action.signal(), transition, postcondition),
                provenance.stream().distinct().toList());
    }

    private String firstFailure(VerificationSignal... signals) {
        for (VerificationSignal signal : signals) {
            if (!signal.passed()) return signal.reason();
        }
        return "live interaction verification passed";
    }
}
