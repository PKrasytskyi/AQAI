package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRunReport;

public interface KnowledgeEnrichmentRunReporter {

    KnowledgeEnrichmentRunReport lastRunReport();
}
