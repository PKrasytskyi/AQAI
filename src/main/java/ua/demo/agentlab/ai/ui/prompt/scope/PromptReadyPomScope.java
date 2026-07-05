package ua.demo.agentlab.ai.ui.prompt.scope;

import java.util.List;

public record PromptReadyPomScope(
        String targetPage,
        String targetRoute,
        boolean requiresAuthentication,
        List<String> prerequisitePages,
        List<String> requirementIds,
        List<String> ownedActions,
        List<PromptReadyAssertion> ownedAssertions,
        List<PromptReadyLocator> allowedLocators,
        List<String> rejectedSuggestions,
        double confidence
) {
    public PromptReadyPomScope {
        targetPage = safe(targetPage);
        targetRoute = safe(targetRoute);
        prerequisitePages = prerequisitePages == null ? List.of() : List.copyOf(prerequisitePages);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        ownedActions = ownedActions == null ? List.of() : List.copyOf(ownedActions);
        ownedAssertions = ownedAssertions == null ? List.of() : List.copyOf(ownedAssertions);
        allowedLocators = allowedLocators == null ? List.of() : List.copyOf(allowedLocators);
        rejectedSuggestions = rejectedSuggestions == null ? List.of() : List.copyOf(rejectedSuggestions);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
