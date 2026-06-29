package ua.demo.agentlab.ui.discovery.model;

import java.util.List;

public record UiDiscoverySnapshot(
        String projectProfileId,
        String projectName,
        String source,
        List<DiscoveredUiPage> pages,
        List<DiscoveredUiFlow> flows
) {
}
