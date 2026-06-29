package ua.demo.agentlab.ui.discovery.model;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;
import ua.demo.agentlab.ui.discovery.identity.PageIdentity;

import java.util.List;

public record DiscoveredUiPage(
        String pageName,
        String route,
        List<String> capabilities,
        List<LocatorHint> locatorHints,
        String discoveryReason,
        CanonicalPageType canonicalPageType,
        PageIdentity pageIdentity
) {
    public DiscoveredUiPage(
            String pageName,
            String route,
            List<String> capabilities,
            List<LocatorHint> locatorHints,
            String discoveryReason
    ) {
        this(
                pageName,
                route,
                capabilities,
                locatorHints,
                discoveryReason,
                CanonicalPageType.fromLegacyPageName(pageName),
                PageIdentity.legacy(pageName, CanonicalPageType.fromLegacyPageName(pageName).mappedType(), route)
        );
    }
}
