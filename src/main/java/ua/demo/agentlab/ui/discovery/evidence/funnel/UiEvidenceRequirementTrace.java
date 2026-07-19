package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.List;

/** BW-04 requirement-scoped trace across the existing discovery and binding stages. */
public record UiEvidenceRequirementTrace(
        UiEvidenceStateReference source,
        List<String> requiredComponentCapabilities,
        List<String> componentIds,
        List<String> expectedActionIntents,
        UiEvidenceStateReference target,
        String discoveryRouteSource
) {
    public UiEvidenceRequirementTrace {
        source = source == null ? UiEvidenceStateReference.empty() : source;
        requiredComponentCapabilities = copy(requiredComponentCapabilities);
        componentIds = copy(componentIds);
        expectedActionIntents = copy(expectedActionIntents);
        target = target == null ? UiEvidenceStateReference.empty() : target;
        discoveryRouteSource = discoveryRouteSource == null || discoveryRouteSource.isBlank()
                ? "UNRESOLVED"
                : discoveryRouteSource.trim();
    }

    public static UiEvidenceRequirementTrace empty() {
        return new UiEvidenceRequirementTrace(
                UiEvidenceStateReference.empty(), List.of(), List.of(), List.of(),
                UiEvidenceStateReference.empty(), "UNRESOLVED");
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }
}
