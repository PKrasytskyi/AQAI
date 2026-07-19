package ua.demo.agentlab.ui.discovery.interaction.observability;

import ua.demo.agentlab.ui.discovery.catalog.ConfirmedUiCatalog;
import ua.demo.agentlab.ui.discovery.interaction.model.InteractionEvidenceStatus;
import ua.demo.agentlab.ui.discovery.interaction.model.LocatorPromotionDecision;
import ua.demo.agentlab.ui.discovery.interaction.persistence.InteractionGraphProjectionResult;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceBundle;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** One immutable trace reused by funnel metrics and diagnostic artifacts. */
public final class EvidenceProjectionTraceAssembler {

    public EvidenceProjectionTrace assemble(
            CanonicalInteractionEvidenceBundle canonical,
            ConfirmedUiCatalog catalog,
            InteractionGraphProjectionResult persistence
    ) {
        if (canonical == null) {
            return new EvidenceProjectionTrace(EvidenceProjectionTrace.SCHEMA_VERSION, "",
                    0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of("canonical evidence missing"));
        }
        Set<String> scoped = canonical.requirementSelection().selected().stream()
                .map(item -> item.candidate().locatorEvidenceId().value()).collect(Collectors.toSet());
        Set<String> topK = canonical.topKSelection().selected().stream()
                .map(item -> item.candidate().locatorEvidenceId().value()).collect(Collectors.toSet());
        Set<String> catalogPrimary = catalog == null ? Set.of() : catalog.pages().stream()
                .flatMap(page -> page.components().stream())
                .flatMap(component -> component.primaryLocators().stream())
                .map(locator -> locator.locatorId()).collect(Collectors.toSet());
        boolean persisted = persistence != null && persistence.executed();
        List<EvidenceProjectionTraceEntry> entries = canonical.promotionDecisions().stream()
                .map(item -> entry(item, scoped, topK, catalogPrimary, persisted)).toList();
        int live = (int) entries.stream().filter(EvidenceProjectionTraceEntry::liveVerified).count();
        int confirmed = (int) entries.stream().filter(item -> "CONFIRMED".equals(item.promotionStatus())).count();
        int primary = (int) entries.stream().filter(EvidenceProjectionTraceEntry::catalogPrimary).count();
        return new EvidenceProjectionTrace(EvidenceProjectionTrace.SCHEMA_VERSION, canonical.runId(),
                canonical.candidates().size(), scoped.size(), topK.size(), live, confirmed,
                persisted ? persistence.locators() : 0, primary, primary, entries,
                List.of("single-source=canonical-interaction-evidence",
                        "graph-is-projection=" + persisted));
    }

    private EvidenceProjectionTraceEntry entry(
            LocatorPromotionDecision decision,
            Set<String> scoped,
            Set<String> topK,
            Set<String> catalogPrimary,
            boolean persisted
    ) {
        var interaction = decision.interaction();
        var candidate = interaction.scopedInteraction().candidate();
        String id = candidate.locatorEvidenceId().value();
        String stoppedAt = "";
        String reason = "";
        if (decision.status() == InteractionEvidenceStatus.REJECTED) {
            stoppedAt = "SAFETY_GATE";
            reason = String.join(",", decision.decisionCodes());
        } else if (!interaction.verification().locatorVerified()) {
            stoppedAt = "LOCATOR_VERIFICATION";
            reason = interaction.verification().reason();
        } else if (!interaction.verification().actionVerified()) {
            stoppedAt = "ACTION_VERIFICATION";
            reason = interaction.verification().reason();
        } else if (!interaction.verification().stateTransitionVerified()) {
            stoppedAt = "STATE_TRANSITION";
            reason = interaction.verification().reason();
        } else if (decision.status() != InteractionEvidenceStatus.CONFIRMED) {
            stoppedAt = "PROMOTION";
            reason = String.join(",", decision.decisionCodes());
        }
        boolean primary = catalogPrimary.contains(id);
        return new EvidenceProjectionTraceEntry(id, candidate.actionKey().value(), candidate.pageId(),
                candidate.componentId(), interaction.scopedInteraction().requirementIds().stream().sorted().toList(),
                true, scoped.contains(id), topK.contains(id), interaction.verification().locatorVerified()
                && interaction.verification().actionVerified(), decision.status().name(), persisted,
                primary, primary, stoppedAt, reason);
    }
}
