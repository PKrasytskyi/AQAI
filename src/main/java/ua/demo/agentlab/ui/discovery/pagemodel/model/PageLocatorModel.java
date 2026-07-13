package ua.demo.agentlab.ui.discovery.pagemodel.model;

public record PageLocatorModel(
        String strategy,
        String value,
        double score,
        String reason,
        boolean unique,
        int observedRuns,
        int totalRuns,
        boolean stableAcrossRuns,
        int browserMatchCount,
        int browserScopedMatchCount,
        String browserScope
) {
    public PageLocatorModel(
            String strategy,
            String value,
            double score,
            String reason,
            boolean unique
    ) {
        this(strategy, value, score, reason, unique, 1, 1, true, -1, -1, "");
    }

    public PageLocatorModel(
            String strategy,
            String value,
            double score,
            String reason,
            boolean unique,
            int observedRuns,
            int totalRuns,
            boolean stableAcrossRuns
    ) {
        this(strategy, value, score, reason, unique, observedRuns, totalRuns, stableAcrossRuns, -1, -1, "");
    }

    public PageLocatorModel {
        strategy = strategy == null ? "" : strategy.trim();
        value = value == null ? "" : value.trim();
        reason = reason == null ? "" : reason.trim();
        score = Math.max(0.0d, Math.min(1.0d, score));
        observedRuns = Math.max(0, observedRuns);
        totalRuns = Math.max(1, totalRuns);
        stableAcrossRuns = totalRuns >= 2 && stableAcrossRuns && observedRuns >= totalRuns;
        browserMatchCount = Math.max(-1, browserMatchCount);
        browserScopedMatchCount = Math.max(-1, browserScopedMatchCount);
        browserScope = browserScope == null ? "" : browserScope.trim();
    }
}
