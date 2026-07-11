package ua.demo.agentlab.ai.comparison;

import java.time.Instant;
import java.util.List;

public record DbImpactComparisonReport(
        String generatedAt,
        String withoutDbRun,
        String withDbRun,
        DbImpactRunMetrics withoutDb,
        DbImpactRunMetrics withDb,
        List<DbImpactMetricRow> rows,
        List<String> notes
) {
    public DbImpactComparisonReport {
        generatedAt = generatedAt == null || generatedAt.isBlank() ? Instant.now().toString() : generatedAt.trim();
        withoutDbRun = withoutDbRun == null ? "" : withoutDbRun.trim();
        withDbRun = withDbRun == null ? "" : withDbRun.trim();
        rows = rows == null ? List.of() : List.copyOf(rows);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
