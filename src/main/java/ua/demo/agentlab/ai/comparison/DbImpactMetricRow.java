package ua.demo.agentlab.ai.comparison;

public record DbImpactMetricRow(
        String metric,
        String withoutDb,
        String withDb,
        String change
) {
    public DbImpactMetricRow {
        metric = clean(metric);
        withoutDb = clean(withoutDb);
        withDb = clean(withDb);
        change = clean(change);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
