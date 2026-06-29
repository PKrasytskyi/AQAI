package ua.demo.agentlab.ui.discovery.selenium.model;

import ua.demo.agentlab.ui.LocatorHint;

public record DiscoveredField(
        String fieldType,
        String name,
        String id,
        String label,
        boolean required,
        String placeholder,
        LocatorHint locatorHint
) {
}
