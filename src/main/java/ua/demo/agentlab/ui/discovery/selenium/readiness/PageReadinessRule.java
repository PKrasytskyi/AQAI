package ua.demo.agentlab.ui.discovery.selenium.readiness;

import ua.demo.agentlab.ui.catalog.PageCapability;

import java.util.List;

public record PageReadinessRule(
        PageCapability capability,
        String route,
        List<String> requiredCssSelectors,
        List<String> readyTextFragments,
        long timeoutMillis,
        String source
) {
    public PageReadinessRule {
        capability = capability == null ? PageCapability.GENERIC : capability;
        route = route == null ? "" : route.trim();
        requiredCssSelectors = normalize(requiredCssSelectors);
        readyTextFragments = normalize(readyTextFragments);
        timeoutMillis = Math.max(500L, timeoutMillis);
        source = source == null || source.isBlank() ? "default" : source.trim();
    }

    public static PageReadinessRule generic(String route, long timeoutMillis) {
        return new PageReadinessRule(
                PageCapability.GENERIC,
                route,
                List.of(),
                List.of(),
                timeoutMillis,
                "generic-dom"
        );
    }

    private static List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
