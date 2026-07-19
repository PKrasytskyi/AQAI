package ua.demo.agentlab.ui.discovery.interaction.selection;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TopKInteractionSelector {

    private final InteractionSafetyGate safetyGate;
    private final int limit;

    public TopKInteractionSelector() {
        this(new InteractionSafetyGate(), 3);
    }

    public TopKInteractionSelector(InteractionSafetyGate safetyGate, int limit) {
        this.safetyGate = safetyGate == null ? new InteractionSafetyGate() : safetyGate;
        this.limit = Math.max(1, limit);
    }

    public TopKInteractionSelection select(List<RequirementScopedInteraction> source) {
        Map<String, List<RequirementScopedInteraction>> groups = new LinkedHashMap<>();
        List<InteractionSafetyDecision> rejected = new ArrayList<>();
        for (RequirementScopedInteraction interaction : source == null
                ? List.<RequirementScopedInteraction>of() : source) {
            InteractionSafetyDecision decision = safetyGate.evaluate(interaction);
            if (!decision.allowed()) {
                rejected.add(decision);
                continue;
            }
            groups.computeIfAbsent(interaction.candidate().actionKey().value(), ignored -> new ArrayList<>())
                    .add(interaction);
        }
        List<RequirementScopedInteraction> selected = new ArrayList<>();
        groups.values().forEach(group -> selected.addAll(selectGroup(group)));
        return new TopKInteractionSelection(TopKInteractionSelection.SCHEMA_VERSION, limit, selected, rejected);
    }

    private List<RequirementScopedInteraction> selectGroup(List<RequirementScopedInteraction> group) {
        List<RequirementScopedInteraction> ranked = group.stream()
                .sorted(Comparator.comparingDouble(
                        (RequirementScopedInteraction item) -> item.scopedScore().finalScore()).reversed()
                        .thenComparing(item -> item.candidate().locatorEvidenceId().value()))
                .toList();
        if (ranked.size() <= 1) return ranked;
        List<RequirementScopedInteraction> selected = new ArrayList<>();
        Set<String> families = new LinkedHashSet<>();
        selected.add(ranked.get(0));
        families.add(safetyGate.selectorFamily(ranked.get(0)));
        ranked.stream().skip(1)
                .filter(item -> !families.contains(safetyGate.selectorFamily(item)))
                .forEach(item -> {
                    if (selected.size() < limit) {
                        selected.add(item);
                        families.add(safetyGate.selectorFamily(item));
                    }
                });
        ranked.stream().skip(1).filter(item -> !selected.contains(item)).forEach(item -> {
            if (selected.size() < limit) selected.add(item);
        });
        return List.copyOf(selected);
    }
}
