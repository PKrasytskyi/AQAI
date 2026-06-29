package ua.demo.agentlab.ui.discovery.identity;

import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;

import java.util.List;

public interface PageNamingResolver {

    PageIdentity resolve(
            CanonicalPageType canonicalPageType,
            String route,
            String title,
            List<String> headings,
            List<String> capabilities,
            List<DiscoveredUiPage> knownPages
    );
}
