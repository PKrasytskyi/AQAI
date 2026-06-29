package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

public record PageKnowledgeCacheLookupOutput(
        PageKnowledgeCacheLookupResult result,
        KnowledgeRunMetadata runMetadata,
        String retrievalMode
) {
}
