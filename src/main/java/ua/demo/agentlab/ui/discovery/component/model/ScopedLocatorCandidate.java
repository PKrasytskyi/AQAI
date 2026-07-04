package ua.demo.agentlab.ui.discovery.component.model;

import java.util.List;

public record ScopedLocatorCandidate(
        String pageId,
        String componentId,
        String elementId,
        String strategy,
        String value,
        int globalMatchCount,
        int scopedMatchCount,
        boolean uniqueOnPage,
        boolean uniqueWithinComponent,
        double uniquenessScore,
        double stabilityScore,
        double readabilityScore,
        double semanticScore,
        double finalScore,
        List<String> risks
) {
    public ScopedLocatorCandidate {
        pageId = safe(pageId);
        componentId = safe(componentId);
        elementId = safe(elementId);
        strategy = safe(strategy);
        value = safe(value);
        globalMatchCount = Math.max(0, globalMatchCount);
        scopedMatchCount = Math.max(0, scopedMatchCount);
        uniquenessScore = clamp(uniquenessScore);
        stabilityScore = clamp(stabilityScore);
        readabilityScore = clamp(readabilityScore);
        semanticScore = clamp(semanticScore);
        finalScore = clamp(finalScore);
        risks = risks == null ? List.of() : List.copyOf(risks.stream()
                .filter(risk -> risk != null && !risk.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : 0.0d;
    }
}
