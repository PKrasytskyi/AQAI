package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.knowledge.model.ExcludedEvidence;

import java.util.List;

public record PromptUiEvidence(
        String targetPage,
        String targetRoute,
        List<String> requirementIds,
        List<PromptActionEvidence> requiredActions,
        List<PromptAssertionEvidence> requiredAssertions,
        List<PromptLocatorEvidence> requiredLocators,
        List<PromptLocatorEvidence> forbiddenLocators,
        List<ExcludedEvidence> excludedEvidence,
        List<String> sourceTrace,
        double confidence
) {
    public PromptUiEvidence {
        targetPage = safe(targetPage);
        targetRoute = safe(targetRoute);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        requiredActions = requiredActions == null ? List.of() : List.copyOf(requiredActions);
        requiredAssertions = requiredAssertions == null ? List.of() : List.copyOf(requiredAssertions);
        requiredLocators = requiredLocators == null ? List.of() : List.copyOf(requiredLocators);
        forbiddenLocators = forbiddenLocators == null ? List.of() : List.copyOf(forbiddenLocators);
        excludedEvidence = excludedEvidence == null ? List.of() : List.copyOf(excludedEvidence);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    public static PromptUiEvidence empty(String reason) {
        return new PromptUiEvidence(
                "",
                "",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(reason == null ? "prompt-evidence:empty" : reason),
                0.0d
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
