package ua.demo.agentlab.ui.flow.model;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;
import ua.demo.agentlab.ui.discovery.identity.PageIdentity;

import java.util.List;

public record CanonicalPage(
        String pageName,
        String route,
        List<String> capabilities,
        List<LocatorHint> locatorHints,
        String source,
        CanonicalPageType canonicalPageType,
        PageIdentity pageIdentity
) {
    public CanonicalPage(
            String pageName,
            String route,
            List<String> capabilities,
            List<LocatorHint> locatorHints,
            String source
    ) {
        this(
                pageName,
                route,
                capabilities,
                locatorHints,
                source,
                CanonicalPageType.fromLegacyPageName(pageName),
                PageIdentity.legacy(pageName, CanonicalPageType.fromLegacyPageName(pageName).mappedType(), route)
        );
    }
}
