package ua.demo.agentlab.ai.schema;

import java.util.List;

public record LlmOutputSchemaValidationReport(
        String schemaVersion,
        List<LlmOutputSchemaIssue> issues
) {
    public LlmOutputSchemaValidationReport {
        schemaVersion = schemaVersion == null ? "" : schemaVersion.trim();
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean valid() {
        return issues.isEmpty();
    }
}
