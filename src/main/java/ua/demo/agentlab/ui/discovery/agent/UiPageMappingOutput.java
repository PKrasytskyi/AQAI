package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeRaw;

public record UiPageMappingOutput(
        MappedUiKnowledgeRaw rawKnowledge,
        MappedUiKnowledgeCurated curatedKnowledge
) {
    public UiPageMappingOutput {
        rawKnowledge = rawKnowledge == null
                ? new MappedUiKnowledgeRaw(null, java.util.List.of())
                : rawKnowledge;
        curatedKnowledge = curatedKnowledge == null
                ? new MappedUiKnowledgeCurated(null, java.util.List.of(), java.util.List.of(), 0.0d)
                : curatedKnowledge;
    }
}
