package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.knowledge.model.ExcludedEvidence;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.List;

public record PromptPageScope(
        MappedPage targetPage,
        String targetPageName,
        String targetRoute,
        boolean requiresAuthentication,
        List<String> prerequisitePages,
        List<String> requirementIds,
        List<ExcludedEvidence> excludedEvidence,
        List<String> sourceTrace
) {
    public PromptPageScope {
        targetPageName = safe(targetPageName);
        targetRoute = safe(targetRoute);
        prerequisitePages = prerequisitePages == null ? List.of() : List.copyOf(prerequisitePages);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        excludedEvidence = excludedEvidence == null ? List.of() : List.copyOf(excludedEvidence);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
