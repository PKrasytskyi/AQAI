package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.util.List;

/** Converts one verified before/action/after tuple into an immutable state edge. */
public final class StateTransitionCaptureService {

    public UiStateTransition capture(
            UiStateSnapshot before,
            UiStateSnapshot after,
            TargetedActionVerification action
    ) {
        if (before == null || after == null || action == null) {
            throw new IllegalArgumentException("State transition requires before, after, and action evidence");
        }
        boolean routeChanged = !before.route().equals(after.route());
        String transitionId = "transition-" + Integer.toHexString(
                (before.stateId() + "|" + action.actionId() + "|" + after.stateId()).hashCode());
        return new UiStateTransition(transitionId, before.stateId(), after.stateId(), action.actionId(),
                action.intent(), action.reason(), routeChanged,
                !routeChanged && !before.stateId().equals(after.stateId()), action.confidence(), before.runMetadata(),
                List.of("spa-state-transition:live-action", "action-verified=" + action.verified()));
    }
}
