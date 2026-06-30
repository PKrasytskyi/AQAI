package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

public record UiDiscoveryArtifactPersistenceInput(
        UiDiscoverySnapshot discoverySnapshot,
        SeleniumDiscoveryResult seleniumDiscoveryResult,
        MappedUiKnowledge mappedUiKnowledge
) {
}
