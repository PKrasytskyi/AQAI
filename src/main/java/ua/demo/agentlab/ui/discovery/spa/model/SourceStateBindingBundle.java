package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

public record SourceStateBindingBundle(
        String schemaVersion,
        KnowledgeRunMetadata runMetadata,
        List<SourceStateBinding> bindings,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "spa-source-state-bindings.v1";

    public SourceStateBindingBundle {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        bindings = bindings == null ? List.of() : List.copyOf(bindings);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }
}
