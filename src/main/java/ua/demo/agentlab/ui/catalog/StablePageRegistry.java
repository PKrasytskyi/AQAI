package ua.demo.agentlab.ui.catalog;

import java.util.List;
import java.util.Optional;

public class StablePageRegistry {

    private final ConfirmedPageRegistry confirmedPages;

    public StablePageRegistry(ConfirmedPageRegistry confirmedPages) {
        this.confirmedPages = confirmedPages == null ? new ConfirmedPageRegistry(List.of()) : confirmedPages;
    }

    public List<ConfirmedPageCandidate> allPages() {
        return confirmedPages.allPages();
    }

    public Optional<ConfirmedPageCandidate> findByRoute(String route) {
        return confirmedPages.findByRoute(route);
    }

    public Optional<ConfirmedPageCandidate> findByPageName(String pageName) {
        return confirmedPages.findByPageName(pageName);
    }

    public boolean hasStableRoute(String route) {
        return findByRoute(route).isPresent();
    }
}
