package ua.demo.agentlab.ui.discovery.interaction.inventory;

import java.util.List;

public record UiInteractionInventoryInvariantReport(
        String schemaVersion,
        boolean passed,
        List<String> violations
) {
    public static final String SCHEMA_VERSION = "ui-interaction-inventory-invariants.v1";

    public UiInteractionInventoryInvariantReport {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        violations = violations == null ? List.of() : List.copyOf(violations);
        passed = violations.isEmpty();
    }
}
