package ua.demo.agentlab.ui.discovery.catalog;

import java.util.List;

public record ConfirmedUiCatalog(
        String schemaVersion,
        String runId,
        boolean complete,
        List<ConfirmedCatalogPage> pages,
        List<String> findings
) {
    public static final String SCHEMA_VERSION = "confirmed-ui-catalog.v1";

    public ConfirmedUiCatalog {
        schemaVersion = safe(schemaVersion).isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        runId = safe(runId);
        pages = pages == null ? List.of() : List.copyOf(pages);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
