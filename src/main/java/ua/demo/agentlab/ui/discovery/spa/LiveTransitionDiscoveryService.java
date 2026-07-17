package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.spa.model.RequirementStateTransition;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Joins live browser action outcomes to before/after states without inferring target routes. */
public final class LiveTransitionDiscoveryService {
    public LiveTransitionDiscovery discover(SourceStateBindingBundle sources,
                                             SpaLiveTargetedVerificationResult verification) {
        if (sources == null || verification == null) {
            return new LiveTransitionDiscovery(LiveTransitionDiscovery.SCHEMA_VERSION, null, verification, List.of(),
                    List.of("live-transition-discovery:missing-input"));
        }
        Map<String, UiStateSnapshot> states = new LinkedHashMap<>();
        verification.stateGraph().states().forEach(state -> states.put(state.stateId(), state));
        List<RequirementStateTransition> results = new ArrayList<>();
        for (SourceStateBinding source : sources.bindings()) {
            TargetedActionVerification action = verification.actionVerifications().stream()
                    .filter(candidate -> candidate.requirementIds().contains(source.requirementId()))
                    .filter(candidate -> source.candidateActionIds().contains(candidate.actionId()))
                    .filter(TargetedActionVerification::verified)
                    .findFirst().orElse(null);
            UiStateTransition transition = action == null ? null : verification.stateGraph().transitions().stream()
                    .filter(candidate -> candidate.actionId().equals(action.actionId()))
                    .findFirst().orElse(null);
            UiStateSnapshot target = transition == null ? null : states.get(transition.toStateId());
            String locatorId = action == null ? "" : source.candidateLocatorIds().stream().filter(locator ->
                    verification.locatorVerifications().stream().anyMatch(candidate -> candidate.verified()
                            && candidate.locatorId().equals(locator)
                            && candidate.requirementIds().contains(source.requirementId()))).findFirst().orElse("");
            boolean confirmed = action != null && transition != null && target != null
                    && (transition.routeChanged() || transition.sameRouteStateChange());
            String reason = confirmed ? "live browser confirmed source action and target state"
                    : failureReason(source, action, transition, target);
            results.add(new RequirementStateTransition(source.requirementId(), source.sourcePageId(), source.sourceRoute(),
                    action == null ? "" : action.actionId(), locatorId,
                    target == null ? "" : target.stateId(), target == null ? "" : target.route(), confirmed, reason));
        }
        return new LiveTransitionDiscovery(LiveTransitionDiscovery.SCHEMA_VERSION, verification.runMetadata(), verification,
                results, List.of("live-transition-discovery:browser-observed", "requirements=" + results.size()));
    }

    private String failureReason(SourceStateBinding source, TargetedActionVerification action,
                                 UiStateTransition transition, UiStateSnapshot target) {
        if (!source.liveVerificationEligible()) return "source state binding was not eligible for live verification";
        if (action == null) return "no requirement-owned action passed live browser verification";
        if (transition == null) return "verified action produced no observable state transition";
        if (target == null) return "transition target state snapshot is missing";
        return "target state did not differ from source state";
    }
}
