package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

/** DB hygiene result. Default policy retires stale evidence instead of deleting it. */
public record SpaEvidenceRetentionResult(boolean executed, int degradedRetired, int orphanRetired,
                                        int deleted, String details, List<String> sourceTrace) {
    public SpaEvidenceRetentionResult {
        details = details == null ? "" : details.trim();
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static SpaEvidenceRetentionResult skipped(String reason) {
        return new SpaEvidenceRetentionResult(false, 0, 0, 0, reason, List.of("spa-retention:skipped"));
    }
}
