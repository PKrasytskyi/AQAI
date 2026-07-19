package ua.demo.agentlab.ui.discovery.interaction.inventory;

import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;

import java.util.List;

public record UiInteractionInventory(
        String schemaVersion,
        SpaDiscoveryMode mode,
        List<UiInteractionPage> pages,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "ui-interaction-inventory.v1";

    public UiInteractionInventory {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        mode = mode == null ? SpaDiscoveryMode.INVENTORY : mode;
        pages = pages == null ? List.of() : List.copyOf(pages);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static UiInteractionInventory empty(SpaDiscoveryMode mode, String reason) {
        return new UiInteractionInventory(SCHEMA_VERSION, mode, List.of(),
                List.of(reason == null ? "interaction-inventory:empty" : reason));
    }
}
