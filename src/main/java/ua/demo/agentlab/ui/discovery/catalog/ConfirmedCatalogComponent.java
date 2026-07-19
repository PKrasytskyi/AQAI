package ua.demo.agentlab.ui.discovery.catalog;

import java.util.List;

public record ConfirmedCatalogComponent(
        String componentId,
        String name,
        String type,
        List<ConfirmedCatalogAction> actions,
        List<ConfirmedCatalogLocator> primaryLocators,
        List<ConfirmedCatalogLocator> standbyLocators
) {
    public ConfirmedCatalogComponent {
        componentId = safe(componentId);
        name = safe(name);
        type = safe(type);
        actions = actions == null ? List.of() : List.copyOf(actions);
        primaryLocators = primaryLocators == null ? List.of() : List.copyOf(primaryLocators);
        standbyLocators = standbyLocators == null ? List.of() : List.copyOf(standbyLocators);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
