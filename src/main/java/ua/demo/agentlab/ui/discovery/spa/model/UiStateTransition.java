package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

/** A live browser-confirmed transition between two SPA states caused by one selected action. */
public record UiStateTransition(
        String transitionId,
        String fromStateId,
        String toStateId,
        String actionId,
        String actionIntent,
        String postcondition,
        boolean routeChanged,
        boolean sameRouteStateChange,
        double confidence,
        KnowledgeRunMetadata runMetadata,
        List<String> sourceTrace
) {
    public UiStateTransition {
        transitionId = safe(transitionId);
        fromStateId = safe(fromStateId);
        toStateId = safe(toStateId);
        actionId = safe(actionId);
        actionIntent = safe(actionIntent);
        postcondition = safe(postcondition);
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
