package ua.demo.agentlab.ai.pageenrichment.service;

import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentInput;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;

import java.util.List;

public interface PageModelEnrichmentClient {

    List<PageModelEnrichmentRecord> enrich(List<PageModelEnrichmentInput> inputs);
}
