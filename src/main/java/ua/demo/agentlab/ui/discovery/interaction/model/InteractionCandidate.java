package ua.demo.agentlab.ui.discovery.interaction.model;

import ua.demo.agentlab.ui.discovery.interaction.identity.LocatorEvidenceId;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticActionKey;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticElementKey;

import java.util.List;

public record InteractionCandidate(
        SemanticElementKey elementKey,
        SemanticActionKey actionKey,
        LocatorEvidenceId locatorEvidenceId,
        String sourceActionId,
        String sourceLocatorId,
        String pageId,
        String route,
        String componentId,
        SemanticAction action,
        String strategy,
        String value,
        boolean visible,
        boolean enabled,
        boolean sameOrigin,
        boolean uniqueOnPage,
        boolean uniqueWithinComponent,
        boolean stableAcrossRuns,
        int globalMatchCount,
        int componentMatchCount,
        double locatorStability,
        double actionCompatibility,
        List<String> risks,
        List<String> provenance,
        ScoreBreakdown intrinsicScore
) {
    public InteractionCandidate {
        if (elementKey == null || actionKey == null || locatorEvidenceId == null || action == null) {
            throw new IllegalArgumentException("Interaction candidate requires typed element, action, and locator identities");
        }
        sourceActionId = safe(sourceActionId);
        sourceLocatorId = safe(sourceLocatorId);
        pageId = safe(pageId);
        route = safe(route);
        componentId = safe(componentId);
        strategy = safe(strategy);
        value = safe(value);
        globalMatchCount = Math.max(0, globalMatchCount);
        componentMatchCount = Math.max(0, componentMatchCount);
        locatorStability = clamp(locatorStability);
        actionCompatibility = clamp(actionCompatibility);
        risks = copy(risks);
        provenance = copy(provenance);
        intrinsicScore = intrinsicScore == null ? new ScoreBreakdown("", java.util.Map.of(), 0.0d) : intrinsicScore;
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static List<String> copy(List<String> value) { return value == null ? List.of() : List.copyOf(value); }
    private static double clamp(double value) { return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d; }
}
