package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;

import java.util.List;

public record SpaInventoryBundle(
        String schemaVersion,
        SpaDiscoveryMode mode,
        List<SpaPageInventory> pages,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "spa-page-inventory.v1";

    public SpaInventoryBundle {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        mode = mode == null ? SpaDiscoveryMode.INVENTORY : mode;
        pages = pages == null ? List.of() : List.copyOf(pages);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static SpaInventoryBundle empty(SpaDiscoveryMode mode, String reason) {
        return new SpaInventoryBundle(SCHEMA_VERSION, mode, List.of(), List.of(reason == null ? "spa-inventory:empty" : reason));
    }
}
