package ua.demo.agentlab.ui.catalog;

import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;

import java.util.List;

public record ConfirmedPageCandidate(
        String pageName,
        String route,
        PageCapability capability,
        PageSource source,
        double confidence,
        List<String> evidence
) {
    public ConfirmedPageCandidate {
        pageName = pageName == null ? "" : pageName.trim();
        route = normalizeRoute(route);
        capability = capability == null ? PageCapability.GENERIC : capability;
        source = source == null ? PageSource.REQUIREMENT_ROUTE : source;
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public boolean hasRoute() {
        return route != null && !route.isBlank();
    }

    private static String normalizeRoute(String value) {
        return RouteCanonicalizer.canonicalize(value);
    }
}
