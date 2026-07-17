package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.List;

/** Keeps current-run verified SPA evidence page-scoped; candidates and fallbacks are excluded. */
public final class CurrentRunSpaLocatorEvidenceSelector {
    public List<PromptLocatorEvidence> select(AiContextPackage context, PromptPageScope scope) {
        if (context == null || scope == null || scope.targetPage() == null
                || context.currentRunSpaLocatorEvidence().isEmpty()) return List.of();
        MappedPage targetPage = scope.targetPage();
        String route = targetPage.urlPattern().isBlank() ? targetPage.url() : targetPage.urlPattern();
        return context.currentRunSpaLocatorEvidence().stream()
                .filter(locator -> locator.sourceTrace().contains("spa-current-run-verified"))
                .filter(locator -> belongsToPage(locator, targetPage, route))
                .filter(locator -> belongsToRequirement(locator, scope.requirementIds()))
                .toList();
    }

    private boolean belongsToPage(PromptLocatorEvidence locator, MappedPage targetPage, String route) {
        boolean pageIdMatches = locator.sourceTrace().stream()
                .anyMatch(trace -> trace.equalsIgnoreCase("spa-page-id:" + targetPage.pageId()));
        boolean routeMatches = locator.sourceTrace().stream()
                .filter(trace -> trace.toLowerCase(java.util.Locale.ROOT).startsWith("spa-route:"))
                .map(trace -> trace.substring("spa-route:".length()))
                .anyMatch(candidate -> RouteCanonicalizer.routeEqualsOrSuffix(candidate, route));
        return pageIdMatches && routeMatches;
    }

    private boolean belongsToRequirement(PromptLocatorEvidence locator, List<String> requirementIds) {
        if (requirementIds == null || requirementIds.isEmpty()) {
            return false;
        }
        return locator.sourceTrace().stream()
                .filter(trace -> trace.toLowerCase(java.util.Locale.ROOT).startsWith("requirement-id:"))
                .map(trace -> trace.substring("requirement-id:".length()))
                .anyMatch(candidate -> requirementIds.stream().anyMatch(candidate::equalsIgnoreCase));
    }
}
