package ua.demo.agentlab.ui.discovery.interaction.model;

import java.util.Map;

public record ScoreBreakdown(String schemaVersion, Map<String, Double> factors, double finalScore) {
    public ScoreBreakdown {
        schemaVersion = schemaVersion == null ? "" : schemaVersion.trim();
        factors = factors == null ? Map.of() : Map.copyOf(factors);
        finalScore = clamp(finalScore);
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d;
    }
}
