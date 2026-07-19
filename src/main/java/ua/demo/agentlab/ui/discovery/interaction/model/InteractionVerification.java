package ua.demo.agentlab.ui.discovery.interaction.model;

import java.util.List;

public record InteractionVerification(
        boolean locatorVerified,
        boolean actionVerified,
        boolean stateTransitionVerified,
        boolean postconditionVerified,
        int globalMatchCount,
        int componentMatchCount,
        double confidence,
        String reason,
        List<String> provenance
) {
    public InteractionVerification {
        globalMatchCount = Math.max(0, globalMatchCount);
        componentMatchCount = Math.max(0, componentMatchCount);
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        reason = reason == null ? "" : reason.trim();
        provenance = provenance == null ? List.of() : List.copyOf(provenance);
    }
}
