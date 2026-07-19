package ua.demo.agentlab.ui.discovery.interaction.pipeline;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.interaction.model.*;
import ua.demo.agentlab.ui.discovery.interaction.mapping.UiInventorySemanticInteractionMapper;
import ua.demo.agentlab.ui.discovery.interaction.scope.RequirementEvidenceSelection;
import ua.demo.agentlab.ui.discovery.interaction.scope.RequirementEvidenceSelector;
import ua.demo.agentlab.ui.discovery.interaction.scoring.InteractionScoringService;
import ua.demo.agentlab.ui.discovery.interaction.promotion.PromotionPolicy;
import ua.demo.agentlab.ui.discovery.interaction.selection.TopKInteractionSelection;
import ua.demo.agentlab.ui.discovery.interaction.selection.TopKInteractionSelector;
import ua.demo.agentlab.ui.discovery.interaction.verification.UiLiveVerificationFacade;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CanonicalInteractionEvidenceAssembler {

    private final UiInventorySemanticInteractionMapper semanticMapper = new UiInventorySemanticInteractionMapper();
    private final RequirementEvidenceSelector requirementSelector = new RequirementEvidenceSelector();
    private final TopKInteractionSelector topKSelector = new TopKInteractionSelector();
    private final InteractionScoringService scoringService = new InteractionScoringService();
    private final UiLiveVerificationFacade liveVerificationFacade = new UiLiveVerificationFacade();
    private final PromotionPolicy promotionPolicy = new PromotionPolicy();
    private final InteractionInvariantGate invariantGate = new InteractionInvariantGate();

    public CanonicalInteractionEvidenceBundle assemble(
            UiInteractionInventory inventory,
            SpaLiveTargetedVerificationResult live,
            List<StructuredBehaviorContract> requirements
    ) {
        List<InteractionCandidate> candidates = semanticMapper.map(inventory);
        RequirementEvidenceSelection requirementSelection = requirementSelector.select(requirements, candidates);
        TopKInteractionSelection topK = topKSelector.select(requirementSelection.selected());
        List<LiveVerifiedInteraction> liveResults = new ArrayList<>();
        for (RequirementScopedInteraction scoped : topK.selected()) {
            InteractionVerification verification = liveVerificationFacade.verify(scoped, live);
            liveResults.add(scoringService.scoreLive(scoped, verification));
        }
        List<LocatorPromotionDecision> decisions = promotionPolicy.decide(liveResults, topK.rejected());
        InteractionInvariantReport invariants = invariantGate.validate(candidates,
                requirementSelection.selected(), topK, decisions);
        invariantGate.enforce(invariants);
        return new CanonicalInteractionEvidenceBundle(
                CanonicalInteractionEvidenceBundle.SCHEMA_VERSION, runId(live), candidates,
                requirementSelection, topK, liveResults, decisions, invariants,
                List.of("candidates=" + candidates.size(),
                        "requirement-scoped=" + requirementSelection.selected().size(),
                        "top-k=" + topK.selected().size(),
                        "live-verified=" + liveResults.stream().filter(this::passed).count(),
                        "confirmed=" + decisions.stream().filter(item -> item.status() == InteractionEvidenceStatus.CONFIRMED).count()));
    }

    private boolean passed(LiveVerifiedInteraction item) {
        return item.verification().locatorVerified() && item.verification().actionVerified()
                && item.verification().stateTransitionVerified() && item.verification().postconditionVerified();
    }

    private String runId(SpaLiveTargetedVerificationResult live) {
        return live == null || live.runMetadata() == null ? "" : live.runMetadata().runId();
    }
}
