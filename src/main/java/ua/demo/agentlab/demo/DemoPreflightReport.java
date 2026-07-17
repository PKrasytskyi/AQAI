package ua.demo.agentlab.demo;

import java.util.List;

public record DemoPreflightReport(
        String schemaVersion,
        String demoId,
        boolean ready,
        List<DemoPreflightIssue> issues
) {
    public static final String SCHEMA_VERSION = "demo-preflight-report.v1";

    public DemoPreflightReport {
        schemaVersion = schemaVersion == null ? SCHEMA_VERSION : schemaVersion.trim();
        demoId = demoId == null ? "" : demoId.trim();
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
