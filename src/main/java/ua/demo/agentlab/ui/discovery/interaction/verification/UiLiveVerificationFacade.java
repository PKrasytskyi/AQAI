package ua.demo.agentlab.ui.discovery.interaction.verification;

import ua.demo.agentlab.ui.discovery.interaction.model.InteractionVerification;
import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;

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
        return verify(scoped, live, null);
    }

    /**
     * A target-state assertion is browser-verified evidence, not a synthetic action. It may
     * promote only the exact locator bound by the target-state stage after a confirmed transition.
     */
    public InteractionVerification verify(
            RequirementScopedInteraction scoped,
            SpaLiveTargetedVerificationResult live,
            TargetStateBindingBundle targetStateBindings
    ) {
        InteractionVerification targetAssertion = targetAssertionVerification(scoped, targetStateBindings);
        if (targetAssertion != null) {
            return targetAssertion;
        }
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

    private InteractionVerification targetAssertionVerification(
            RequirementScopedInteraction scoped,
            TargetStateBindingBundle targetStateBindings
    ) {
        if (scoped == null || scoped.candidate().action() != ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction.READ
                || targetStateBindings == null) {
            return null;
        }
        boolean matched = scoped.requirementIds().stream().anyMatch(requirementId -> targetStateBindings.behaviorBindings().stream()
                .filter(binding -> binding.requirementId().equalsIgnoreCase(requirementId))
                .filter(binding -> binding.executable())
                .filter(binding -> binding.pageId().equalsIgnoreCase(scoped.candidate().pageId()))
                .filter(binding -> binding.route().equalsIgnoreCase(scoped.candidate().route()))
                .flatMap(binding -> binding.assertions().stream())
                .anyMatch(assertion -> assertion.verifiable()
                        && assertion.locatorId().equals(scoped.candidate().sourceLocatorId())));
        if (!matched) {
            return null;
        }
        return new InteractionVerification(true, true, true, true,
                scoped.candidate().globalMatchCount(), scoped.candidate().componentMatchCount(), 0.90d,
                "live target-state assertion binding verified",
                java.util.List.of("target-state-binding", "typed-assertion-observation",
                        "locator-id:" + scoped.candidate().sourceLocatorId()));
    }

    private String firstFailure(VerificationSignal... signals) {
        for (VerificationSignal signal : signals) {
            if (!signal.passed()) return signal.reason();
        }
        return "live interaction verification passed";
    }
}
