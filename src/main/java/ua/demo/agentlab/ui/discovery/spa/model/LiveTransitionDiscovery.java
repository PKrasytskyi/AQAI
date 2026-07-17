package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

public record LiveTransitionDiscovery(
        String schemaVersion,
        KnowledgeRunMetadata runMetadata,
        SpaLiveTargetedVerificationResult verification,
        List<RequirementStateTransition> transitions,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "spa-live-transition-discovery.v1";

    public LiveTransitionDiscovery {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }
}
