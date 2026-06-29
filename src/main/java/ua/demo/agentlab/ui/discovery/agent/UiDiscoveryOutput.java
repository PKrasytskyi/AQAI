package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.ui.discovery.model.UiDiscoveryResult;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

public record UiDiscoveryOutput(
        UiDiscoverySnapshot discoverySnapshot,
        SeleniumDiscoveryResult seleniumDiscoveryResult,
        CanonicalPageFlowModel canonicalPageFlowModel
) {
    public static UiDiscoveryOutput from(UiDiscoveryResult result, CanonicalPageFlowModel canonicalPageFlowModel) {
        if (result == null) {
            throw new IllegalArgumentException("discovery result cannot be null");
        }
        return new UiDiscoveryOutput(
                result.snapshot(),
                result.seleniumDiscoveryResult(),
                canonicalPageFlowModel
        );
    }
}
