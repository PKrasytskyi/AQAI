package ua.demo.agentlab.ui.discovery.pagemodel.model;

public record PageLocatorModel(
        String strategy,
        String value,
        double score,
        String reason,
        boolean unique,
        int observedRuns,
        int totalRuns,
        boolean stableAcrossRuns
) {
    public PageLocatorModel(
            String strategy,
            String value,
            double score,
            String reason,
            boolean unique
    ) {
        this(strategy, value, score, reason, unique, 1, 1, true);
    }

    public PageLocatorModel {
        strategy = strategy == null ? "" : strategy.trim();
        value = value == null ? "" : value.trim();
        reason = reason == null ? "" : reason.trim();
        score = Math.max(0.0d, Math.min(1.0d, score));
        observedRuns = Math.max(0, observedRuns);
        totalRuns = Math.max(1, totalRuns);
        stableAcrossRuns = totalRuns <= 1 || stableAcrossRuns && observedRuns >= totalRuns;
    }
}
