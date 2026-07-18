package ua.demo.agentlab.ui.discovery.interaction.verification;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.util.Set;

public final class StateTransitionVerifier {

    private static final Set<SemanticAction> TRANSITION_ACTIONS = Set.of(
            SemanticAction.NAVIGATE, SemanticAction.LOGOUT, SemanticAction.OPEN_MODAL,
            SemanticAction.OPEN_MENU, SemanticAction.FILTER, SemanticAction.SEARCH,
            SemanticAction.SORT_COLLECTION, SemanticAction.PAGINATE,
            SemanticAction.OPEN_RECORD, SemanticAction.CONFIRM_ACTION);

    public VerificationSignal verify(RequirementScopedInteraction scoped, SpaLiveTargetedVerificationResult live) {
        if (!TRANSITION_ACTIONS.contains(scoped.candidate().action())) {
            return new VerificationSignal(true, 1.0d, "state transition is not required",
                    java.util.List.of("state-transition-verifier:not-required"));
        }
        String sourceActionId = scoped.candidate().sourceActionId();
        UiStateTransition transition = live == null || live.stateGraph() == null ? null
                : live.stateGraph().transitions().stream()
                .filter(item -> item.actionId().equals(sourceActionId))
                .filter(item -> item.actionIntent().equalsIgnoreCase(scoped.candidate().action().name()))
                .findFirst().orElse(null);
        if (transition == null) {
            return VerificationSignal.failed("observable state transition is missing for "
                    + scoped.candidate().action(), "state-transition-verifier");
        }
        boolean changed = !transition.fromStateId().isBlank()
                && !transition.toStateId().isBlank()
                && !transition.fromStateId().equals(transition.toStateId())
                && (transition.routeChanged() || transition.sameRouteStateChange());
        return new VerificationSignal(changed, changed ? transition.confidence() : 0.0d,
                changed ? "browser-observed state transition passed" : "state transition did not change observable UI state",
                java.util.List.of("state-transition-verifier", "transition-id:" + transition.transitionId()));
    }

    public boolean requiresTransition(SemanticAction action) {
        return TRANSITION_ACTIONS.contains(action);
    }

}
