package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentFailure;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public record PageModelEnrichmentOutput(
        List<PageModelEnrichmentRecord> records,
        List<PageModelEnrichmentRecord> cachedRecords,
        List<PageModelEnrichmentRecord> generatedRecords,
        MappedUiKnowledge enrichedMappedUiKnowledge,
        List<String> failures,
        List<PageModelEnrichmentFailure> failureDetails,
        int openAiAttempts,
        int openAiSuccesses,
        int openAiFailures,
        int openAiFallbacks,
        int promptChars,
        int responseChars,
        int actualInputTokens,
        int actualOutputTokens,
        int actualTotalTokens
) {
    public PageModelEnrichmentOutput {
        records = records == null ? List.of() : List.copyOf(records);
        cachedRecords = cachedRecords == null ? List.of() : List.copyOf(cachedRecords);
        generatedRecords = generatedRecords == null ? List.of() : List.copyOf(generatedRecords);
        failures = failures == null ? List.of() : List.copyOf(failures);
        failureDetails = failureDetails == null ? List.of() : List.copyOf(failureDetails);
        openAiAttempts = Math.max(0, openAiAttempts);
        openAiSuccesses = Math.max(0, openAiSuccesses);
        openAiFailures = Math.max(0, openAiFailures);
        openAiFallbacks = Math.max(0, openAiFallbacks);
        promptChars = Math.max(0, promptChars);
        responseChars = Math.max(0, responseChars);
        actualInputTokens = Math.max(0, actualInputTokens);
        actualOutputTokens = Math.max(0, actualOutputTokens);
        actualTotalTokens = Math.max(0, actualTotalTokens);
    }
}
