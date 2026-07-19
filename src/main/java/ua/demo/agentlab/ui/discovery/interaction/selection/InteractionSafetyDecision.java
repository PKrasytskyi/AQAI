package ua.demo.agentlab.ui.discovery.interaction.selection;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;

import java.util.List;

public record InteractionSafetyDecision(
        RequirementScopedInteraction interaction,
        boolean allowed,
        List<String> rejectionCodes
) {
    public InteractionSafetyDecision {
        if (interaction == null) throw new IllegalArgumentException("Safety decision requires interaction evidence");
        rejectionCodes = rejectionCodes == null ? List.of() : List.copyOf(rejectionCodes);
    }
}
