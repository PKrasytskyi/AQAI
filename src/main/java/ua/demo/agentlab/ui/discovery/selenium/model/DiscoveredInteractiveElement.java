package ua.demo.agentlab.ui.discovery.selenium.model;

import ua.demo.agentlab.ui.LocatorHint;

public record DiscoveredInteractiveElement(
        String elementType,
        String visibleText,
        String role,
        String name,
        String id,
        String href,
        boolean enabled,
        boolean visible,
        LocatorHint locatorHint
) {
}
