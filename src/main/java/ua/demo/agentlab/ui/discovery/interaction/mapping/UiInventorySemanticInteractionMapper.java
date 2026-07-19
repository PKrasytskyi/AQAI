package ua.demo.agentlab.ui.discovery.interaction.mapping;

import ua.demo.agentlab.ui.discovery.interaction.identity.LocatorEvidenceId;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticActionKey;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticElementKey;
import ua.demo.agentlab.ui.discovery.interaction.model.InteractionCandidate;
import ua.demo.agentlab.ui.discovery.interaction.model.ScoreBreakdown;
import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;
import ua.demo.agentlab.ui.discovery.interaction.scoring.InteractionScoringService;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Canonical semantic boundary from discovered component inventory to interaction candidates. */
public final class UiInventorySemanticInteractionMapper {

    private final InteractionScoringService scoringService;

    public UiInventorySemanticInteractionMapper() {
        this(new InteractionScoringService());
    }

    public UiInventorySemanticInteractionMapper(InteractionScoringService scoringService) {
        this.scoringService = scoringService == null ? new InteractionScoringService() : scoringService;
    }

    public List<InteractionCandidate> map(UiInteractionInventory inventory) {
        if (inventory == null) return List.of();
        List<InteractionCandidate> result = new ArrayList<>();
        inventory.pages().forEach(page -> page.components().forEach(component ->
                component.actions().forEach(action -> result.addAll(map(
                        page.pageId(), page.pageName(), page.route(), page.capability(),
                        page.pageFingerprintHash(), component, action)))));
        return List.copyOf(result.stream().collect(java.util.stream.Collectors.toMap(
                item -> item.actionKey().value() + "|" + item.locatorEvidenceId().value(),
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new)).values());
    }

    private List<InteractionCandidate> map(
            String pageId,
            String pageName,
            String route,
            String capability,
            String pageFingerprint,
            SemanticComponentInventory component,
            CandidateActionEvidence action
    ) {
        SemanticAction semanticAction = semanticAction(action.intent());
        if (semanticAction == null || action.targetElementId().isBlank()) return List.of();
        String semanticRole = compactElementId(action.targetElementId());
        SemanticElementKey elementKey = SemanticElementKey.of(pageId, route, component.componentId(), semanticRole);
        SemanticActionKey actionKey = new SemanticActionKey(elementKey, semanticAction.name());
        List<InteractionCandidate> result = new ArrayList<>();
        for (CandidateLocatorEvidence locator : component.locators()) {
            if (!action.requiredLocatorIds().contains(locator.locatorId())) continue;
            List<String> risks = new ArrayList<>(component.risks());
            risks.addAll(locator.risks());
            List<String> provenance = new ArrayList<>(List.of(
                    "semantic-mapper:interaction-inventory",
                    "discovery-action-id:" + action.actionId(),
                    "discovery-locator-id:" + locator.locatorId(),
                    "page-name:" + pageName,
                    "page-capability:" + capability,
                    "page-fingerprint:" + pageFingerprint));
            provenance.addAll(action.sourceTrace());
            InteractionCandidate candidate = new InteractionCandidate(
                    elementKey,
                    actionKey,
                    LocatorEvidenceId.of(elementKey, locator.strategy(), locator.value()),
                    action.actionId(),
                    locator.locatorId(),
                    pageId,
                    route,
                    component.componentId(),
                    semanticAction,
                    locator.strategy(),
                    locator.value(),
                    true,
                    true,
                    locator.sameOrigin(),
                    locator.uniqueOnPage(),
                    locator.uniqueWithinComponent(),
                    locator.stableAcrossRuns(),
                    locator.globalMatchCount(),
                    locator.componentMatchCount(),
                    locator.qualityScore(),
                    action.confidence(),
                    List.copyOf(new LinkedHashSet<>(risks)),
                    List.copyOf(new LinkedHashSet<>(provenance)),
                    new ScoreBreakdown("", Map.of(), 0.0d));
            result.add(scoringService.scoreIntrinsic(candidate));
        }
        return result;
    }

    private SemanticAction semanticAction(String value) {
        try {
            return SemanticAction.valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String compactElementId(String value) {
        int marker = value.lastIndexOf(":element:");
        return marker >= 0 ? value.substring(marker + ":element:".length()) : value;
    }
}
