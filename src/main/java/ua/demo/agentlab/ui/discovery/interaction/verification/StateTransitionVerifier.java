package ua.demo.agentlab.ui.discovery.interaction.verification;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

public final class StateTransitionVerifier {

    private final VerificationStrategyResolver strategyResolver;

    public StateTransitionVerifier() {
        this(new VerificationStrategyResolver());
    }

    StateTransitionVerifier(VerificationStrategyResolver strategyResolver) {
        this.strategyResolver = java.util.Objects.requireNonNull(strategyResolver);
    }

    public VerificationSignal verify(RequirementScopedInteraction scoped, SpaLiveTargetedVerificationResult live) {
        TransitionVerificationStrategy strategy = strategyResolver.resolve(scoped, live);
        if (strategy == TransitionVerificationStrategy.NONE) {
            return new VerificationSignal(true, 1.0d, "state transition is not required",
                    java.util.List.of("state-transition-verifier:not-required"));
        }
        UiStateTransition transition = strategyResolver.observedTransition(scoped, live);
        if (transition == null) {
            return VerificationSignal.failed(strategy == TransitionVerificationStrategy.DOCUMENT_NAVIGATION
                            ? "document navigation evidence is missing for " + scoped.candidate().action()
                            : "observable UI state transition is missing for " + scoped.candidate().action(),
                    "state-transition-verifier:" + strategy.name().toLowerCase(java.util.Locale.ROOT));
        }
        boolean stateChanged = !transition.fromStateId().isBlank()
                && !transition.toStateId().isBlank()
                && !transition.fromStateId().equals(transition.toStateId());
        boolean passed = stateChanged && (strategy == TransitionVerificationStrategy.DOCUMENT_NAVIGATION
                ? transition.routeChanged()
                : transition.sameRouteStateChange());
        return new VerificationSignal(passed, passed ? transition.confidence() : 0.0d,
                passed ? strategy.name().toLowerCase(java.util.Locale.ROOT) + " verification passed"
                        : "observed transition does not satisfy " + strategy.name().toLowerCase(java.util.Locale.ROOT),
                java.util.List.of("state-transition-verifier",
                        "strategy:" + strategy.name(),
                        "transition-id:" + transition.transitionId()));
    }

    public boolean requiresTransition(ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction action) {
        return strategyResolver.requiresTransition(action);
    }

}
