package ua.demo.agentlab.ui.discovery.mapping.model;

import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;

import java.util.List;

public record LocatorCandidate(
        LocatorStrategy strategy,
        String value,
        double stabilityScore,
        String evidenceSource,
        String elementRole,
        String accessibleName,
        String visibleText,
        String href,
        String originHost,
        boolean sameOrigin,
        boolean uniqueOnPage,
        boolean stableAcrossRuns,
        List<String> risks
) {
    public LocatorCandidate {
        strategy = strategy == null ? LocatorStrategy.UNKNOWN : strategy;
        value = value == null ? "" : value.trim();
        stabilityScore = Math.max(0.0d, Math.min(1.0d, stabilityScore));
        evidenceSource = evidenceSource == null ? "" : evidenceSource.trim();
        elementRole = elementRole == null ? "" : elementRole.trim();
        accessibleName = accessibleName == null ? "" : accessibleName.trim();
        visibleText = visibleText == null ? "" : visibleText.trim();
        href = href == null ? "" : href.trim();
        originHost = originHost == null ? "" : originHost.trim().toLowerCase();
        risks = risks == null ? List.of() : List.copyOf(risks.stream()
                .filter(risk -> risk != null && !risk.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }
}
