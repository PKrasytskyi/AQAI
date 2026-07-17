package ua.demo.agentlab.ui.discovery.selenium.model;

import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationResult;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SeleniumDiscoveryResult(
        String baseUrl,
        List<DiscoveredPageSnapshot> pages,
        List<DiscoveredTransition> transitions,
        int discoveryRunCount,
        Map<String, Integer> locatorObservationCounts,
        List<DiscoveryAuthenticationResult> authenticationResults,
        List<PageReadinessResult> readinessResults
) {
    public SeleniumDiscoveryResult(
            String baseUrl,
            List<DiscoveredPageSnapshot> pages,
            List<DiscoveredTransition> transitions
    ) {
        this(baseUrl, pages, transitions, 1, Map.of(), List.of(), List.of());
    }

    public SeleniumDiscoveryResult(
            String baseUrl,
            List<DiscoveredPageSnapshot> pages,
            List<DiscoveredTransition> transitions,
            int discoveryRunCount,
            Map<String, Integer> locatorObservationCounts
    ) {
        this(baseUrl, pages, transitions, discoveryRunCount, locatorObservationCounts, List.of(), List.of());
    }

    public SeleniumDiscoveryResult(
            String baseUrl,
            List<DiscoveredPageSnapshot> pages,
            List<DiscoveredTransition> transitions,
            int discoveryRunCount,
            Map<String, Integer> locatorObservationCounts,
            List<DiscoveryAuthenticationResult> authenticationResults
    ) {
        this(baseUrl, pages, transitions, discoveryRunCount, locatorObservationCounts, authenticationResults, List.of());
    }

    public SeleniumDiscoveryResult {
        baseUrl = baseUrl == null ? "" : baseUrl.trim();
        pages = pages == null ? List.of() : List.copyOf(pages);
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
        discoveryRunCount = Math.max(1, discoveryRunCount);
        locatorObservationCounts = locatorObservationCounts == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(locatorObservationCounts));
        authenticationResults = authenticationResults == null ? List.of() : List.copyOf(authenticationResults);
        readinessResults = readinessResults == null ? List.of() : List.copyOf(readinessResults);
    }
}
