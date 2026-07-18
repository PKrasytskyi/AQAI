package ua.demo.agentlab.ui.discovery.interaction.selection;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;

import java.util.List;

public record TopKInteractionSelection(
        String schemaVersion,
        int limitPerSemanticAction,
        List<RequirementScopedInteraction> selected,
        List<InteractionSafetyDecision> rejected
) {
    public static final String SCHEMA_VERSION = "interaction-top-k.v1";

    public TopKInteractionSelection {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        limitPerSemanticAction = Math.max(1, limitPerSemanticAction);
        selected = selected == null ? List.of() : List.copyOf(selected);
        rejected = rejected == null ? List.of() : List.copyOf(rejected);
    }
}
