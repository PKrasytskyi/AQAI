package ua.demo.agentlab.ui.discovery.interaction.verification;

import java.util.List;

public record VerificationSignal(boolean passed, double confidence, String reason, List<String> provenance) {
    public VerificationSignal {
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        reason = reason == null ? "" : reason.trim();
        provenance = provenance == null ? List.of() : List.copyOf(provenance);
    }

    public static VerificationSignal failed(String reason, String source) {
        return new VerificationSignal(false, 0.0d, reason, List.of(source));
    }
}
