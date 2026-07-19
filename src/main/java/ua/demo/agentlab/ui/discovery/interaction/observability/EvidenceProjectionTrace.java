package ua.demo.agentlab.ui.discovery.interaction.observability;

import java.util.List;

public record EvidenceProjectionTrace(
        String schemaVersion,
        String runId,
        int rawCandidates,
        int requirementScoped,
        int topK,
        int liveVerified,
        int confirmed,
        int persisted,
        int catalogPrimary,
        int promptAllowed,
        List<EvidenceProjectionTraceEntry> entries,
        List<String> findings
) {
    public static final String SCHEMA_VERSION = "evidence-projection-trace.v1";
    public EvidenceProjectionTrace {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        runId = runId == null ? "" : runId.trim();
        entries = entries == null ? List.of() : List.copyOf(entries);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
