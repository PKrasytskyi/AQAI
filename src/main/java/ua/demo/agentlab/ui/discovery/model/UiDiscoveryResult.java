package ua.demo.agentlab.ui.discovery.model;

import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

public record UiDiscoveryResult(
        UiDiscoverySnapshot snapshot,
        SeleniumDiscoveryResult seleniumDiscoveryResult
) {
    public UiDiscoveryResult {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }
    }
}
