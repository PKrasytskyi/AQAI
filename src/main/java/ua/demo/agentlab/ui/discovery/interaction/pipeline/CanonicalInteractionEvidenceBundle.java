package ua.demo.agentlab.ui.discovery.interaction.pipeline;

import ua.demo.agentlab.ui.discovery.interaction.model.InteractionCandidate;
import ua.demo.agentlab.ui.discovery.interaction.model.LiveVerifiedInteraction;
import ua.demo.agentlab.ui.discovery.interaction.model.LocatorPromotionDecision;
import ua.demo.agentlab.ui.discovery.interaction.scope.RequirementEvidenceSelection;
import ua.demo.agentlab.ui.discovery.interaction.selection.TopKInteractionSelection;

import java.util.List;

public record CanonicalInteractionEvidenceBundle(
        String schemaVersion,
        String runId,
        List<InteractionCandidate> candidates,
        RequirementEvidenceSelection requirementSelection,
        TopKInteractionSelection topKSelection,
        List<LiveVerifiedInteraction> liveResults,
        List<LocatorPromotionDecision> promotionDecisions,
        InteractionInvariantReport invariants,
        List<String> findings
) {
    public static final String SCHEMA_VERSION = "canonical-interaction-evidence.v1";

    public CanonicalInteractionEvidenceBundle {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        runId = runId == null ? "" : runId.trim();
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        liveResults = liveResults == null ? List.of() : List.copyOf(liveResults);
        promotionDecisions = promotionDecisions == null ? List.of() : List.copyOf(promotionDecisions);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
