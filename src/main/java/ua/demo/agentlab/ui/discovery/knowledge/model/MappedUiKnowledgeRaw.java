package ua.demo.agentlab.ui.discovery.knowledge.model;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public record MappedUiKnowledgeRaw(
        MappedUiKnowledge knowledge,
        List<String> sourceTrace
) {
    public MappedUiKnowledgeRaw {
        knowledge = knowledge == null ? MappedUiKnowledge.empty() : knowledge;
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }
}
