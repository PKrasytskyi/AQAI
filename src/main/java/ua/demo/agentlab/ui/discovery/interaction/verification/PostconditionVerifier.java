package ua.demo.agentlab.ui.discovery.interaction.verification;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;

public final class PostconditionVerifier {

    public VerificationSignal verify(
            RequirementScopedInteraction scoped,
            VerificationSignal action,
            VerificationSignal transition,
            boolean transitionRequired
    ) {
        if (!action.passed()) {
            return VerificationSignal.failed("postcondition cannot pass because the action failed", "postcondition-verifier");
        }
        if (transitionRequired && !transition.passed()) {
            return VerificationSignal.failed("required state transition postcondition was not observed", "postcondition-verifier");
        }
        double confidence = transitionRequired
                ? Math.min(action.confidence(), transition.confidence())
                : action.confidence();
        return new VerificationSignal(true, confidence, "typed postcondition passed",
                java.util.List.of("postcondition-verifier", "semantic-action:" + scoped.candidate().action()));
    }
}
