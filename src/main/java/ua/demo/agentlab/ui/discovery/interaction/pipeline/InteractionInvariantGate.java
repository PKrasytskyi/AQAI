package ua.demo.agentlab.ui.discovery.interaction.pipeline;

import ua.demo.agentlab.ui.discovery.interaction.model.InteractionCandidate;
import ua.demo.agentlab.ui.discovery.interaction.model.LocatorPromotionDecision;
import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.interaction.selection.TopKInteractionSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InteractionInvariantGate {

    public InteractionInvariantReport validate(
            List<InteractionCandidate> candidates,
            List<RequirementScopedInteraction> scoped,
            TopKInteractionSelection topK,
            List<LocatorPromotionDecision> decisions
    ) {
        List<String> violations = new ArrayList<>();
        for (InteractionCandidate candidate : candidates) {
            if (!candidate.actionKey().elementKey().equals(candidate.elementKey())) {
                violations.add("ACTION_ELEMENT_IDENTITY_MISMATCH:" + candidate.actionKey().value());
            }
            if (!candidate.locatorEvidenceId().elementKey().equals(candidate.elementKey())) {
                violations.add("LOCATOR_ELEMENT_IDENTITY_MISMATCH:" + candidate.locatorEvidenceId().value());
            }
        }
        scoped.stream().filter(item -> item.requirementIds().isEmpty())
                .forEach(item -> violations.add("MISSING_REQUIREMENT_OWNERSHIP:" + item.candidate().actionKey().value()));
        Map<String, Integer> perAction = new HashMap<>();
        topK.selected().forEach(item -> perAction.merge(item.candidate().actionKey().value(), 1, Integer::sum));
        perAction.forEach((action, count) -> {
            if (count > topK.limitPerSemanticAction()) violations.add("TOP_K_LIMIT_EXCEEDED:" + action);
        });
        decisions.stream().filter(LocatorPromotionDecision::primary)
                .filter(item -> !item.interaction().verification().locatorVerified()
                        || !item.interaction().verification().actionVerified())
                .forEach(item -> violations.add("UNVERIFIED_PRIMARY:" +
                        item.interaction().scopedInteraction().candidate().locatorEvidenceId().value()));
        return new InteractionInvariantReport(InteractionInvariantReport.SCHEMA_VERSION,
                violations.isEmpty(), violations);
    }

    public void enforce(InteractionInvariantReport report) {
        if (report != null && !report.passed()) {
            throw new IllegalStateException("Canonical interaction invariant gate failed: "
                    + String.join("; ", report.violations()));
        }
    }
}
