package ua.demo.agentlab.ui.discovery.interaction.pipeline;

import java.util.List;

public record InteractionInvariantReport(String schemaVersion, boolean passed, List<String> violations) {
    public static final String SCHEMA_VERSION = "interaction-invariants.v1";

    public InteractionInvariantReport {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        violations = violations == null ? List.of() : List.copyOf(violations);
        passed = violations.isEmpty();
    }
}
