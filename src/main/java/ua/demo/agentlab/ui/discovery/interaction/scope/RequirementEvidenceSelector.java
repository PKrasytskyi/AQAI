package ua.demo.agentlab.ui.discovery.interaction.scope;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.interaction.model.InteractionCandidate;
import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;
import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;
import ua.demo.agentlab.ui.discovery.interaction.scoring.InteractionScoringService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Selects requirement-owned interactions without page-first or route fallback behavior. */
public final class RequirementEvidenceSelector {

    private final RequirementActionIntentResolver intentResolver;
    private final InteractionScoringService scoringService;

    public RequirementEvidenceSelector() {
        this(new RequirementActionIntentResolver(), new InteractionScoringService());
    }

    public RequirementEvidenceSelector(
            RequirementActionIntentResolver intentResolver,
            InteractionScoringService scoringService
    ) {
        this.intentResolver = intentResolver == null ? new RequirementActionIntentResolver() : intentResolver;
        this.scoringService = scoringService == null ? new InteractionScoringService() : scoringService;
    }

    public RequirementEvidenceSelection select(
            List<StructuredBehaviorContract> requirements,
            List<InteractionCandidate> candidates
    ) {
        Map<String, Aggregate> aggregated = new LinkedHashMap<>();
        Map<String, String> unresolved = new LinkedHashMap<>();
        for (StructuredBehaviorContract requirement : requirements == null
                ? List.<StructuredBehaviorContract>of() : requirements) {
            if (requirement == null || !requirement.executable()) continue;
            Set<SemanticAction> requiredActions = intentResolver.resolve(requirement);
            if (requiredActions.isEmpty()) {
                unresolved.put(requirement.requirementId(), "no typed action intent resolved from requirement");
                continue;
            }
            int before = aggregated.size();
            for (InteractionCandidate candidate : candidates == null ? List.<InteractionCandidate>of() : candidates) {
                if (!requiredActions.contains(candidate.action())) continue;
                double routeMatch = routeMatch(requirement.targetContext(), candidate.route());
                double ownership = ownership(requirement.targetContext(), candidate);
                if (routeMatch < 0.50d && ownership < 0.50d) continue;
                double relevance = semanticRelevance(requirement, candidate);
                String key = candidate.actionKey().value() + "|" + candidate.locatorEvidenceId().value();
                aggregated.computeIfAbsent(key, ignored -> new Aggregate(candidate))
                        .add(requirement.requirementId(), relevance, routeMatch, ownership);
            }
            boolean found = aggregated.values().stream().anyMatch(value -> value.requirementIds.contains(requirement.requirementId()));
            if (!found || aggregated.size() == before && !found) {
                unresolved.put(requirement.requirementId(), "no interaction matched typed action and confirmed target context");
            }
        }
        List<RequirementScopedInteraction> selected = aggregated.values().stream()
                .map(value -> scoringService.scoreScoped(value.candidate, value.requirementIds,
                        value.relevance, value.routeMatch, value.ownership))
                .sorted(java.util.Comparator.comparingDouble(
                        (RequirementScopedInteraction item) -> item.scopedScore().finalScore()).reversed())
                .toList();
        return new RequirementEvidenceSelection(RequirementEvidenceSelection.SCHEMA_VERSION, selected, unresolved);
    }

    private double routeMatch(String context, String route) {
        String expected = extractRoute(context);
        if (expected.isBlank()) return 0.70d;
        return RouteCanonicalizer.routeEqualsOrSuffix(expected, route) ? 1.0d : 0.0d;
    }

    private double ownership(String context, InteractionCandidate candidate) {
        String normalized = normalize(context);
        if (normalized.contains(normalize(candidate.pageId())) || normalized.contains(normalize(candidate.componentId()))) {
            return 1.0d;
        }
        return routeMatch(context, candidate.route()) >= 1.0d ? 0.90d : 0.35d;
    }

    private double semanticRelevance(StructuredBehaviorContract requirement, InteractionCandidate candidate) {
        String material = normalize(requirement.capability() + " " + String.join(" ", requirement.actions())
                + " " + requirement.targetContext());
        String role = normalize(candidate.elementKey().semanticRole());
        if (!role.isBlank() && material.contains(role.replace('-', ' '))) return 1.0d;
        return 0.85d;
    }

    private String extractRoute(String context) {
        if (context == null) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(/[A-Za-z0-9._~!$&'()*+,;=:@%/-]+)").matcher(context);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', ' ');
    }

    private static final class Aggregate {
        private final InteractionCandidate candidate;
        private final Set<String> requirementIds = new LinkedHashSet<>();
        private double relevance;
        private double routeMatch;
        private double ownership;

        private Aggregate(InteractionCandidate candidate) { this.candidate = candidate; }

        private void add(String requirementId, double relevance, double routeMatch, double ownership) {
            requirementIds.add(requirementId);
            this.relevance = Math.max(this.relevance, relevance);
            this.routeMatch = Math.max(this.routeMatch, routeMatch);
            this.ownership = Math.max(this.ownership, ownership);
        }
    }
}
