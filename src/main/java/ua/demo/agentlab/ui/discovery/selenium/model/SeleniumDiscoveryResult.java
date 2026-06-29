package ua.demo.agentlab.ui.discovery.selenium.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SeleniumDiscoveryResult(
        String baseUrl,
        List<DiscoveredPageSnapshot> pages,
        List<DiscoveredTransition> transitions,
        int discoveryRunCount,
        Map<String, Integer> locatorObservationCounts
) {
    public SeleniumDiscoveryResult(
            String baseUrl,
            List<DiscoveredPageSnapshot> pages,
            List<DiscoveredTransition> transitions
    ) {
        this(baseUrl, pages, transitions, 1, Map.of());
    }

    public SeleniumDiscoveryResult {
        baseUrl = baseUrl == null ? "" : baseUrl.trim();
        pages = pages == null ? List.of() : List.copyOf(pages);
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
        discoveryRunCount = Math.max(1, discoveryRunCount);
        locatorObservationCounts = locatorObservationCounts == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(locatorObservationCounts));
    }
}
