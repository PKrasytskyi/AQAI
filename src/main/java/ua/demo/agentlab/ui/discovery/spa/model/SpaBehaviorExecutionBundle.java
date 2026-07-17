package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import java.util.List;

/** Run-scoped results for structured SPA behavior execution. */
public record SpaBehaviorExecutionBundle(String schemaVersion, KnowledgeRunMetadata runMetadata,
                                         List<SpaBehaviorExecutionResult> results, List<String> sourceTrace) {
    public static final String SCHEMA_VERSION = "spa-structured-behavior-execution.v1";
    public SpaBehaviorExecutionBundle {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        results = results == null ? List.of() : List.copyOf(results); sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }
}
