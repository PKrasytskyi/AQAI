package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRecord;

import java.util.List;

public interface KnowledgeEnrichmentClient {

    List<KnowledgeEnrichmentRecord> enrich(KnowledgeEnrichmentRequest request);
}
