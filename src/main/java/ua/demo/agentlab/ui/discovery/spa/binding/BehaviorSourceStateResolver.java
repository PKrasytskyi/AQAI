package ua.demo.agentlab.ui.discovery.spa.binding;

import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.util.List;
import java.util.Optional;

/** Resolves only a previously confirmed source-state binding. */
public final class BehaviorSourceStateResolver {
    public Optional<SpaPageInventory> resolve(List<SpaPageInventory> pages, SourceStateBinding source) {
        if (pages == null || source == null || source.sourcePageId().isBlank()) return Optional.empty();
        return pages.stream().filter(page -> page.pageId().equalsIgnoreCase(source.sourcePageId())
                || routeMatches(page.route(), source.sourceRoute())).findFirst();
    }

    private boolean routeMatches(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        return !a.isBlank() && !b.isBlank() && (a.equals(b) || a.endsWith(b) || b.endsWith(a));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
