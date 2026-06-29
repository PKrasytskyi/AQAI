package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

public record UiPageModelInput(
        UiDiscoverySnapshot discoverySnapshot,
        SeleniumDiscoveryResult seleniumDiscoveryResult
) {
}
