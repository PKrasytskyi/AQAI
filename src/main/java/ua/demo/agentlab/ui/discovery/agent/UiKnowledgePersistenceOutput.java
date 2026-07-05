package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeWriteResult;

import java.util.List;

public record UiKnowledgePersistenceOutput(
        List<PageKnowledgeWriteResult> results,
        KnowledgeRunMetadata runMetadata,
        String phase
) {
    public UiKnowledgePersistenceOutput {
        results = results == null ? List.of() : List.copyOf(results);
        phase = phase == null || phase.isBlank() ? "unknown" : phase.trim();
    }
}
