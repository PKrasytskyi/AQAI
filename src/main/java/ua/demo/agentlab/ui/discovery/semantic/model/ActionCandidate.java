package ua.demo.agentlab.ui.discovery.semantic.model;

import java.util.List;
import java.util.Locale;

public record ActionCandidate(
        String action,
        String targetElementId,
        double confidence,
        List<String> evidence
) {
    public ActionCandidate {
        action = safe(action).toUpperCase(Locale.ROOT);
        targetElementId = safe(targetElementId);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
