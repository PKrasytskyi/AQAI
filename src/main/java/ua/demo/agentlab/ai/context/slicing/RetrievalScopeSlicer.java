package ua.demo.agentlab.ai.context.slicing;

import ua.demo.agentlab.ai.context.*;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Projects retrieval metadata before prompt assembly; raw retrieval text remains outside the prompt. */
public final class RetrievalScopeSlicer {

    public LocatorEvidenceScope sliceLocatorEvidence(AiContextPackage context, MappedUiKnowledge mapped) {
        if (context == null || mapped == null) return new LocatorEvidenceScope(List.of(), List.of());
        return new LocatorEvidenceScope(
                byPage(context.dbStableLocatorEvidence(), mapped, "db-page-id:", "db-route:"),
                byPage(context.confirmedCatalogLocatorEvidence(), mapped, "catalog-page-id:", "catalog-route:"));
    }

    public UiKnowledgeRetrievalContext slice(AiContextPackage context,
                                              TargetPageScopeResolver.TargetPageScope target) {
        UiKnowledgeRetrievalContext source = context.retrievalContext();
        if (source == null) return UiKnowledgeRetrievalContext.empty("Retrieval context is not available");
        Set<String> pageNames = target.requested().targetPageNames().stream().map(this::normalize)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> pageIds = context.mappedUiKnowledge() == null ? Set.of()
                : context.mappedUiKnowledge().pages().stream()
                .filter(page -> !target.routeGuard().hasConfirmedPages()
                        || target.routeGuard().isConfirmed(page.pageName(), page.urlPattern())
                        || target.routeGuard().isConfirmed(page.pageName(), page.url()))
                .filter(page -> target.requested().targetRoutes().isEmpty()
                        ? pageNames.isEmpty() || PageReferenceMatcher.matchesAny(page, pageNames)
                        : target.requested().targetRoutes().stream().anyMatch(route ->
                        PageReferenceMatcher.routeMatches(route, page.urlPattern())
                                || PageReferenceMatcher.routeMatches(route, page.url())))
                .map(page -> normalize(page.pageId()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<RetrievedChunk> vectors = source.vectorMatches().stream()
                .filter(match -> matches(match, pageIds, pageNames)).limit(6).toList();
        List<UiKnowledgeGraphMatch> graph = source.graphMatches().stream()
                .filter(match -> pageIds.contains(normalize(match.pageId()))
                        || pageNames.contains(normalize(match.pageId()))
                        || pageNames.contains(normalize(match.name())))
                .limit(8).toList();
        return new UiKnowledgeRetrievalContext(source.query(), source.queryTerms(), vectors, graph,
                source.vectorSource(), source.graphSource(), source.notes());
    }

    private boolean matches(RetrievedChunk chunk, Set<String> pageIds, Set<String> pageNames) {
        if (chunk == null || chunk.metadata() == null || chunk.metadata().tags() == null) return false;
        return chunk.metadata().tags().stream().map(this::normalize)
                .anyMatch(tag -> pageIds.contains(tag) || pageNames.contains(tag))
                || pageNames.contains(normalize(chunk.metadata().artifactName()))
                || pageNames.contains(normalize(chunk.metadata().packageName()));
    }

    private List<PromptLocatorEvidence> byPage(List<PromptLocatorEvidence> source,
                                               MappedUiKnowledge mapped,
                                               String pagePrefix,
                                               String routePrefix) {
        if (source == null || source.isEmpty()) return List.of();
        Set<String> pageIds = mapped.pages().stream().map(MappedPage::pageId).map(this::normalizeEvidence)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> routes = mapped.pages().stream()
                .flatMap(page -> java.util.stream.Stream.of(page.urlPattern(), page.url()))
                .map(this::normalizeEvidence).filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return source.stream().filter(locator -> locator.sourceTrace().stream().anyMatch(trace -> {
            String value = normalizeEvidence(trace);
            return pageIds.stream().anyMatch(page -> value.equals(pagePrefix + page))
                    || routes.stream().anyMatch(route -> value.equals(routePrefix + route));
        })).toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeEvidence(String value) {
        return value == null ? "" : value.trim();
    }

    public record LocatorEvidenceScope(List<PromptLocatorEvidence> dbStable,
                                       List<PromptLocatorEvidence> confirmedCatalog) {
        public LocatorEvidenceScope {
            dbStable = dbStable == null ? List.of() : List.copyOf(dbStable);
            confirmedCatalog = confirmedCatalog == null ? List.of() : List.copyOf(confirmedCatalog);
        }
    }
}
