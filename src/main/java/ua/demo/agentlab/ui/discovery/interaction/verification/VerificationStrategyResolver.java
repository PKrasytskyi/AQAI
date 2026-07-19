package ua.demo.agentlab.ui.discovery.interaction.verification;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.util.Set;

/** Selects how one canonical interaction proves its observable postcondition. */
public final class VerificationStrategyResolver {

    private static final Set<SemanticAction> DOCUMENT_NAVIGATION_ACTIONS = Set.of(
            SemanticAction.NAVIGATE,
            SemanticAction.LOGOUT
    );
    private static final Set<SemanticAction> UI_STATE_ACTIONS = Set.of(
            SemanticAction.OPEN_MODAL,
            SemanticAction.OPEN_MENU,
            SemanticAction.FILTER,
            SemanticAction.SEARCH,
            SemanticAction.SORT_COLLECTION,
            SemanticAction.PAGINATE,
            SemanticAction.OPEN_RECORD,
            SemanticAction.CONFIRM_ACTION
    );

    public TransitionVerificationStrategy resolve(
            RequirementScopedInteraction scoped,
            SpaLiveTargetedVerificationResult live
    ) {
        SemanticAction action = scoped.candidate().action();
        if (!requiresTransition(action)) {
            return TransitionVerificationStrategy.NONE;
        }
        if (DOCUMENT_NAVIGATION_ACTIONS.contains(action)) {
            return TransitionVerificationStrategy.DOCUMENT_NAVIGATION;
        }
        UiStateTransition observed = observedTransition(scoped, live);
        if (observed != null && observed.routeChanged()) {
            return TransitionVerificationStrategy.DOCUMENT_NAVIGATION;
        }
        if (observed != null && observed.sameRouteStateChange()) {
            return TransitionVerificationStrategy.UI_STATE_TRANSITION;
        }
        return TransitionVerificationStrategy.UI_STATE_TRANSITION;
    }

    public boolean requiresTransition(SemanticAction action) {
        return DOCUMENT_NAVIGATION_ACTIONS.contains(action) || UI_STATE_ACTIONS.contains(action);
    }

    public UiStateTransition observedTransition(
            RequirementScopedInteraction scoped,
            SpaLiveTargetedVerificationResult live
    ) {
        String sourceActionId = scoped.candidate().sourceActionId();
        return live == null || live.stateGraph() == null ? null
                : live.stateGraph().transitions().stream()
                .filter(item -> item.actionId().equals(sourceActionId))
                .filter(item -> item.actionIntent().equalsIgnoreCase(scoped.candidate().action().name()))
                .findFirst()
                .orElse(null);
    }
}
