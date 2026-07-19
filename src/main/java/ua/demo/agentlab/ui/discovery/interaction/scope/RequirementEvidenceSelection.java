package ua.demo.agentlab.ui.discovery.interaction.scope;

import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;

import java.util.List;
import java.util.Map;

public record RequirementEvidenceSelection(
        String schemaVersion,
        List<RequirementScopedInteraction> selected,
        Map<String, String> unresolvedRequirements
) {
    public static final String SCHEMA_VERSION = "requirement-interaction-selection.v1";

    public RequirementEvidenceSelection {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        selected = selected == null ? List.of() : List.copyOf(selected);
        unresolvedRequirements = unresolvedRequirements == null ? Map.of() : Map.copyOf(unresolvedRequirements);
    }
}
