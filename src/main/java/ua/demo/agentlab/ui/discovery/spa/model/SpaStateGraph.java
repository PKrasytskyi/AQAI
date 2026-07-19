package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

/** Current-run state graph assembled only from live, requirement-scoped browser actions. */
public record SpaStateGraph(
        String schemaVersion,
        KnowledgeRunMetadata runMetadata,
        List<UiStateSnapshot> states,
        List<UiStateTransition> transitions,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "spa-state-graph.v1";

    public SpaStateGraph {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        states = states == null ? List.of() : List.copyOf(states);
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public static SpaStateGraph empty(KnowledgeRunMetadata metadata, String reason) {
        return new SpaStateGraph(SCHEMA_VERSION, metadata, List.of(), List.of(), List.of(reason));
    }
}
