package ua.demo.agentlab.ui.catalog;

import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

public class ConfirmedRouteGuard {

    private final ConfirmedPageRegistry registry;

    public ConfirmedRouteGuard(ConfirmedPageRegistry registry) {
        this.registry = registry == null ? new ConfirmedPageRegistry(java.util.List.of()) : registry;
    }

    public boolean hasConfirmedPages() {
        return !registry.allPages().isEmpty();
    }

    public boolean isConfirmed(String pageName, String routeOrUrl) {
        if (isConfirmedRoute(routeOrUrl)) {
            return true;
        }
        if (pageName == null || pageName.isBlank()) {
            return false;
        }
        return registry.findByPageName(pageName).isPresent();
    }

    public boolean isConfirmedRoute(String routeOrUrl) {
        if (routeOrUrl == null || routeOrUrl.isBlank()) {
            return false;
        }
        return registry.allPages().stream()
                .anyMatch(page -> PageReferenceMatcher.routeMatches(page.route(), routeOrUrl)
                        || PageReferenceMatcher.routeMatches(routeOrUrl, page.route()));
    }
}
