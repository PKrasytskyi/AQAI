package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.List;
import java.util.Locale;

/** Keeps canonical confirmed locator evidence page and route scoped. */
public final class ConfirmedCatalogLocatorEvidenceSelector {
    public List<PromptLocatorEvidence> select(AiContextPackage context, PromptPageScope scope) {
        if (context == null || scope == null || scope.targetPage() == null
                || context.confirmedCatalogLocatorEvidence().isEmpty()) return List.of();
        MappedPage targetPage = scope.targetPage();
        String route = targetPage.urlPattern().isBlank() ? targetPage.url() : targetPage.urlPattern();
        return context.confirmedCatalogLocatorEvidence().stream()
                .filter(locator -> locator.sourceTrace().contains("confirmed-catalog-primary"))
                .filter(locator -> belongsToPage(locator, targetPage, route))
                .toList();
    }

    private boolean belongsToPage(PromptLocatorEvidence locator, MappedPage targetPage, String route) {
        boolean pageIdMatches = locator.sourceTrace().stream()
                .anyMatch(trace -> trace.equalsIgnoreCase("catalog-page-id:" + targetPage.pageId()));
        boolean pageNameMatches = locator.sourceTrace().stream()
                .anyMatch(trace -> trace.equalsIgnoreCase("catalog-page-name:" + targetPage.pageName()));
        boolean routeMatches = locator.sourceTrace().stream()
                .filter(trace -> trace.toLowerCase(Locale.ROOT).startsWith("catalog-route:"))
                .map(trace -> trace.substring("catalog-route:".length()))
                .anyMatch(candidate -> RouteCanonicalizer.routeEqualsOrSuffix(candidate, route));
        return (pageIdMatches || pageNameMatches) && routeMatches;
    }
}
