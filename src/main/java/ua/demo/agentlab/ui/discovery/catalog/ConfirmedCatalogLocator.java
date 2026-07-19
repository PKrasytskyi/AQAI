package ua.demo.agentlab.ui.discovery.catalog;

import java.util.List;

public record ConfirmedCatalogLocator(
        String locatorId,
        String elementId,
        String strategy,
        String value,
        double score,
        String evidenceType,
        boolean sameOrigin,
        boolean browserVerified,
        boolean stable,
        boolean unique,
        List<String> sourceTrace
) {
    public ConfirmedCatalogLocator {
        locatorId = safe(locatorId);
        elementId = safe(elementId);
        strategy = safe(strategy);
        value = safe(value);
        score = Double.isFinite(score) ? Math.max(0.0d, Math.min(1.0d, score)) : 0.0d;
        evidenceType = safe(evidenceType);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
