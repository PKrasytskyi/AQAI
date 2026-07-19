package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

/** Browser-verifiable action prerequisites, independent from an LLM-generated POM. */
public record ComponentInteractionGraph(
        String schemaVersion,
        KnowledgeRunMetadata runMetadata,
        List<ComponentActionDependency> dependencies,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "spa-component-interaction-graph.v1";

    public ComponentInteractionGraph {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static ComponentInteractionGraph empty(KnowledgeRunMetadata metadata, String reason) {
        return new ComponentInteractionGraph(SCHEMA_VERSION, metadata, List.of(),
                List.of(reason == null ? "spa-component-graph:empty" : reason));
    }
}
