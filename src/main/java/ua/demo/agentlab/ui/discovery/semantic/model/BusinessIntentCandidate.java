package ua.demo.agentlab.ui.discovery.semantic.model;

import java.util.List;
import java.util.Locale;

public record BusinessIntentCandidate(
        String intent,
        double confidence,
        List<String> evidence,
        boolean needsReview
) {
    public BusinessIntentCandidate {
        intent = safe(intent).toUpperCase(Locale.ROOT);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
