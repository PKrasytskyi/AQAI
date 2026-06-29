package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

/**
 * Runtime evidence for one enrichment pass. It deliberately contains no prompt
 * content or credentials, so it is safe to persist with AI-run artifacts.
 */
public record KnowledgeEnrichmentRunReport(
        String source,
        int baselineRecordCount,
        int requestedBatches,
        int completedBatches,
        int enrichedRecordCount,
        List<String> failures
) {
    public KnowledgeEnrichmentRunReport {
        source = source == null || source.isBlank() ? "rule-based" : source.trim();
        baselineRecordCount = Math.max(0, baselineRecordCount);
        requestedBatches = Math.max(0, requestedBatches);
        completedBatches = Math.max(0, completedBatches);
        enrichedRecordCount = Math.max(0, enrichedRecordCount);
        failures = failures == null ? List.of() : List.copyOf(failures);
    }

    public boolean usedOpenAi() {
        return "openai".equals(source) && completedBatches > 0;
    }

    public static KnowledgeEnrichmentRunReport notStarted(String source) {
        return new KnowledgeEnrichmentRunReport(source, 0, 0, 0, 0, List.of());
    }
}
