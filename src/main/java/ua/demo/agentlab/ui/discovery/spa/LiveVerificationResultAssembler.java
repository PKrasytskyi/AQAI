package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.spa.model.*;

import java.util.List;

/** Owns final pass semantics and result construction for live targeted verification. */
public final class LiveVerificationResultAssembler {

    public SpaLiveTargetedVerificationResult success(
            KnowledgeRunMetadata metadata,
            List<TargetedLocatorVerification> locators,
            List<TargetedActionVerification> actions,
            List<UiStateSnapshot> states,
            List<UiStateTransition> transitions,
            List<LiveTargetPageSnapshot> targetPages,
            List<String> trace
    ) {
        boolean passed = locators.stream().allMatch(TargetedLocatorVerification::verified)
                && actions.stream().allMatch(TargetedActionVerification::verified);
        return result(metadata, passed, locators, actions, states, transitions, targetPages, trace,
                "spa-state-graph:live-targeted-verification");
    }

    public SpaLiveTargetedVerificationResult failure(
            KnowledgeRunMetadata metadata,
            List<TargetedLocatorVerification> locators,
            List<TargetedActionVerification> actions,
            List<UiStateSnapshot> states,
            List<UiStateTransition> transitions,
            List<LiveTargetPageSnapshot> targetPages,
            List<String> trace
    ) {
        return result(metadata, false, locators, actions, states, transitions, targetPages, trace,
                "spa-state-graph:live-browser-error");
    }

    private SpaLiveTargetedVerificationResult result(
            KnowledgeRunMetadata metadata,
            boolean passed,
            List<TargetedLocatorVerification> locators,
            List<TargetedActionVerification> actions,
            List<UiStateSnapshot> states,
            List<UiStateTransition> transitions,
            List<LiveTargetPageSnapshot> targetPages,
            List<String> trace,
            String graphTrace
    ) {
        return new SpaLiveTargetedVerificationResult(SpaLiveTargetedVerificationResult.SCHEMA_VERSION,
                metadata, true, passed, locators, actions,
                new SpaStateGraph(SpaStateGraph.SCHEMA_VERSION, metadata, states, transitions, List.of(graphTrace)),
                targetPages, trace);
    }
}
