package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.List;

/** Run-level acceptance report for requirement-to-POM evidence continuity. */
public record UiEvidenceFunnelReport(
        String schemaVersion,
        String runId,
        boolean completenessPassed,
        boolean pomReadinessPassed,
        UiEvidenceFunnelMetrics metrics,
        List<UiEvidenceRequirementResult> requirements,
        List<String> findings
) {
    public static final String SCHEMA_VERSION = "ui-evidence-funnel.v2";

    public UiEvidenceFunnelReport {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? SCHEMA_VERSION
                : schemaVersion.trim();
        runId = runId == null ? "" : runId.trim();
        metrics = metrics == null ? new UiEvidenceFunnelMetrics(0, 0, 0, 0, 0, 0, 0) : metrics;
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
