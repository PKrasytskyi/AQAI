package ua.demo.agentlab.ui.discovery.interaction.model;

import java.util.List;

public record LocatorPromotionDecision(
        LiveVerifiedInteraction interaction,
        InteractionEvidenceStatus status,
        boolean primary,
        boolean standby,
        List<String> decisionCodes
) {
    public LocatorPromotionDecision {
        if (interaction == null || status == null) {
            throw new IllegalArgumentException("Promotion decision requires verified interaction and status");
        }
        if (primary && standby) throw new IllegalArgumentException("One locator cannot be primary and standby");
        decisionCodes = decisionCodes == null ? List.of() : List.copyOf(decisionCodes);
    }
}
