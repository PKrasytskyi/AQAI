package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

public record CandidateActionEvidence(
        String actionId,
        String componentId,
        String intent,
        String targetElementId,
        double confidence,
        List<String> requiredLocatorIds,
        List<String> preconditions,
        List<String> postconditions,
        List<String> sourceTrace,
        SpaEvidenceStatus status
) {
    public CandidateActionEvidence {
        actionId = safe(actionId);
        componentId = safe(componentId);
        intent = safe(intent);
        targetElementId = safe(targetElementId);
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        requiredLocatorIds = copy(requiredLocatorIds);
        preconditions = copy(preconditions);
        postconditions = copy(postconditions);
        sourceTrace = copy(sourceTrace);
        status = status == null ? SpaEvidenceStatus.CANDIDATE : status;
    }

    private static List<String> copy(List<String> values) { return values == null ? List.of() : List.copyOf(values); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
