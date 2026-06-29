package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public record PageModelEnrichmentOutput(
        List<PageModelEnrichmentRecord> records,
        List<PageModelEnrichmentRecord> cachedRecords,
        List<PageModelEnrichmentRecord> generatedRecords,
        MappedUiKnowledge enrichedMappedUiKnowledge,
        List<String> failures
) {
    public PageModelEnrichmentOutput {
        records = records == null ? List.of() : List.copyOf(records);
        cachedRecords = cachedRecords == null ? List.of() : List.copyOf(cachedRecords);
        generatedRecords = generatedRecords == null ? List.of() : List.copyOf(generatedRecords);
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
}
