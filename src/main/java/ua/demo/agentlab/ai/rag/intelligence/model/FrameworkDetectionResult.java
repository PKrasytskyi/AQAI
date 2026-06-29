package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.Map;
import java.util.Set;

public record FrameworkDetectionResult(
        Set<FrameworkType> frameworks,
        Map<FrameworkType, String> evidence
) {
    public FrameworkDetectionResult {
        frameworks = frameworks == null ? Set.of(FrameworkType.UNKNOWN) : Set.copyOf(frameworks);
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
