package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

public record SpaTargetedVerificationResult(
        String schemaVersion,
        KnowledgeRunMetadata runMetadata,
        List<TargetedLocatorVerification> locatorVerifications,
        List<TargetedActionVerification> actionVerifications,
        List<String> excludedEvidence,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "spa-targeted-verification.v1";

    public SpaTargetedVerificationResult {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        locatorVerifications = locatorVerifications == null ? List.of() : List.copyOf(locatorVerifications);
        actionVerifications = actionVerifications == null ? List.of() : List.copyOf(actionVerifications);
        excludedEvidence = excludedEvidence == null ? List.of() : List.copyOf(excludedEvidence);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static SpaTargetedVerificationResult empty(KnowledgeRunMetadata metadata, String reason) {
        return new SpaTargetedVerificationResult(SCHEMA_VERSION, metadata, List.of(), List.of(), List.of(),
                List.of(reason == null ? "spa-targeted-verification:empty" : reason));
    }
}
