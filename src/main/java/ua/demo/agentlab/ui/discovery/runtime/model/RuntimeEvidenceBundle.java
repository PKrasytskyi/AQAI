package ua.demo.agentlab.ui.discovery.runtime.model;

import java.util.List;

public record RuntimeEvidenceBundle(
        List<NetworkRequestEvent> networkRequests,
        List<NetworkResponseEvent> networkResponses,
        List<ConsoleLogEvent> consoleLogs,
        List<NavigationEvent> navigationEvents,
        List<DomMutationEvent> domMutations,
        List<SemanticNetworkEvidence> semanticNetworkEvidence,
        List<RuntimeStateTransition> stateTransitions,
        List<String> sourceTrace
) {
    public RuntimeEvidenceBundle {
        networkRequests = networkRequests == null ? List.of() : List.copyOf(networkRequests);
        networkResponses = networkResponses == null ? List.of() : List.copyOf(networkResponses);
        consoleLogs = consoleLogs == null ? List.of() : List.copyOf(consoleLogs);
        navigationEvents = navigationEvents == null ? List.of() : List.copyOf(navigationEvents);
        domMutations = domMutations == null ? List.of() : List.copyOf(domMutations);
        semanticNetworkEvidence = semanticNetworkEvidence == null ? List.of() : List.copyOf(semanticNetworkEvidence);
        stateTransitions = stateTransitions == null ? List.of() : List.copyOf(stateTransitions);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static RuntimeEvidenceBundle empty() {
        return new RuntimeEvidenceBundle(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
