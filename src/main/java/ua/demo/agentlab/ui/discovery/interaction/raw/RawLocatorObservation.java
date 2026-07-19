package ua.demo.agentlab.ui.discovery.interaction.raw;

import java.util.List;

/** Browser/DOM observation only. It carries no promotion or POM decision. */
public record RawLocatorObservation(
        String observationId,
        String strategy,
        String value,
        boolean sameOrigin,
        int globalMatchCount,
        int containerMatchCount,
        List<String> observedRisks
) {
    public RawLocatorObservation {
        observationId = safe(observationId);
        strategy = safe(strategy);
        value = safe(value);
        globalMatchCount = Math.max(0, globalMatchCount);
        containerMatchCount = Math.max(0, containerMatchCount);
        observedRisks = observedRisks == null ? List.of() : List.copyOf(observedRisks);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
