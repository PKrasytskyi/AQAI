package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

public record TargetStateBindingBundle(
        String schemaVersion,
        KnowledgeRunMetadata runMetadata,
        List<TargetStateBinding> targets,
        List<BoundSpaBehaviorContract> behaviorBindings,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "spa-target-state-bindings.v1";

    public TargetStateBindingBundle {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        targets = targets == null ? List.of() : List.copyOf(targets);
        behaviorBindings = behaviorBindings == null ? List.of() : List.copyOf(behaviorBindings);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }
}
