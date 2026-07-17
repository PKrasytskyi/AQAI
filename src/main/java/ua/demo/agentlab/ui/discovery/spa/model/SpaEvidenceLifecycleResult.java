package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

public record SpaEvidenceLifecycleResult(
        boolean executed,
        int verifiedLocators,
        int failedLocators,
        int verifiedActions,
        int failedActions,
        int confirmedEvidence,
        int degradedEvidence,
        String details,
        List<String> sourceTrace
) {
    public SpaEvidenceLifecycleResult {
        details = details == null ? "" : details.trim();
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static SpaEvidenceLifecycleResult skipped(String details) {
        return new SpaEvidenceLifecycleResult(false, 0, 0, 0, 0, 0, 0, details, List.of("spa-lifecycle:skipped"));
    }
}
