package ua.demo.agentlab.ui.discovery.interaction.compatibility;

import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;

import java.util.List;

public record ActionCompatibilityResult(
        SemanticAction action,
        boolean compatible,
        double confidence,
        List<String> reasons
) {
    public ActionCompatibilityResult {
        if (action == null) throw new IllegalArgumentException("Action is required");
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
