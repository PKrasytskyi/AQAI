package ua.demo.agentlab.ai.context;

import java.util.List;

public record CanonicalUiInteractionModel(List<CanonicalUiInteraction> interactions) {
    public CanonicalUiInteractionModel {
        interactions = interactions == null ? List.of() : List.copyOf(interactions);
    }

    public static CanonicalUiInteractionModel empty() {
        return new CanonicalUiInteractionModel(List.of());
    }
}
