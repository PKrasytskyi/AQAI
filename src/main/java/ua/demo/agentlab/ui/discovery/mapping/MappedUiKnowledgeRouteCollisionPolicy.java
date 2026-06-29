package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.catalog.ConfirmedPageCandidate;
import ua.demo.agentlab.ui.catalog.StablePageRegistry;
import ua.demo.agentlab.ui.discovery.identity.PageIdentity;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.AssertionHint;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class MappedUiKnowledgeRouteCollisionPolicy {

    private final PageKnowledgeArtifactBuilder artifactBuilder = new PageKnowledgeArtifactBuilder();

    public MappedUiKnowledge apply(MappedUiKnowledge knowledge, StablePageRegistry registry) {
        if (knowledge == null || knowledge.pages().isEmpty()) {
            return knowledge;
        }
        Map<String, List<MappedPage>> pagesByRoute = new LinkedHashMap<>();
        for (MappedPage page : knowledge.pages()) {
            pagesByRoute.computeIfAbsent(normalizeRoute(route(page)), ignored -> new ArrayList<>()).add(page);
        }

        List<MappedPage> resolvedPages = new ArrayList<>();
        Map<String, String> pageIdRedirects = new LinkedHashMap<>();
        for (Map.Entry<String, List<MappedPage>> entry : pagesByRoute.entrySet()) {
            List<MappedPage> candidates = entry.getValue();
            MappedPage primary = choosePrimary(candidates, registry.findByRoute(entry.getKey()).orElse(null));
            MappedPage merged = mergeAndCanonicalize(primary, candidates, registry.findByRoute(entry.getKey()).orElse(null));
            resolvedPages.add(merged);
            for (MappedPage candidate : candidates) {
                pageIdRedirects.put(candidate.pageId(), merged.pageId());
            }
        }

        List<MappedTransition> transitions = knowledge.transitions().stream()
                .map(transition -> remapTransition(transition, pageIdRedirects))
                .filter(transition -> !transition.fromPageId().equals(transition.toPageId()))
                .distinct()
                .toList();
        return new MappedUiKnowledge(
                resolvedPages,
                transitions,
                artifactBuilder.buildGraphNodes(resolvedPages),
                artifactBuilder.buildGraphEdges(resolvedPages, transitions),
                artifactBuilder.buildVectorDocuments(resolvedPages, transitions)
        );
    }

    private MappedPage choosePrimary(List<MappedPage> candidates, ConfirmedPageCandidate confirmed) {
        return candidates.stream()
                .max(Comparator.comparingDouble(page -> score(page, confirmed)))
                .orElse(candidates.get(0));
    }

    private double score(MappedPage page, ConfirmedPageCandidate confirmed) {
        double score = page.elements().size() + page.forms().size() * 3.0d + page.actions().size();
        if (confirmed != null) {
            if (normalize(page.pageName()).equals(normalize(confirmed.pageName()))) {
                score += 100.0d;
            }
            if (page.canonicalPageType() == confirmed.capability().canonicalPageType()) {
                score += 50.0d;
            }
        }
        return score;
    }

    private MappedPage mergeAndCanonicalize(MappedPage primary, List<MappedPage> candidates, ConfirmedPageCandidate confirmed) {
        String pageName = confirmed == null ? primary.pageName() : confirmed.pageName();
        var canonicalPageType = confirmed == null ? primary.canonicalPageType() : confirmed.capability().canonicalPageType();
        String pageType = canonicalPageType.mappedType();
        String route = confirmed == null ? primary.urlPattern() : confirmed.route();
        List<MappedElement> elements = merge(candidates, MappedPage::elements, MappedElement::elementId);
        List<MappedForm> forms = merge(candidates, MappedPage::forms, MappedForm::formId);
        List<MappedAction> actions = merge(candidates, MappedPage::actions, MappedAction::actionId);
        List<AssertionHint> assertionHints = merge(candidates, MappedPage::assertionHints,
                hint -> hint.hintType() + "|" + hint.target());
        return new MappedPage(
                primary.pageId(),
                pageName,
                pageType,
                primary.url(),
                route,
                primary.title(),
                primary.sections(),
                elements,
                forms,
                actions,
                assertionHints,
                primary.stateHints(),
                primary.screenshotPath(),
                primary.htmlPath(),
                canonicalPageType,
                PageIdentity.legacy(pageName, pageType, route)
        );
    }

    private <T> List<T> merge(List<MappedPage> pages, Function<MappedPage, List<T>> extractor, Function<T, String> keyExtractor) {
        Map<String, T> merged = new LinkedHashMap<>();
        for (MappedPage page : pages) {
            for (T item : extractor.apply(page)) {
                merged.putIfAbsent(keyExtractor.apply(item), item);
            }
        }
        return List.copyOf(merged.values());
    }

    private MappedTransition remapTransition(MappedTransition transition, Map<String, String> pageIdRedirects) {
        return new MappedTransition(
                transition.transitionId(),
                pageIdRedirects.getOrDefault(transition.fromPageId(), transition.fromPageId()),
                transition.actionId(),
                pageIdRedirects.getOrDefault(transition.toPageId(), transition.toPageId()),
                transition.toUrl(),
                transition.actionType(),
                transition.success(),
                transition.confidenceScore()
        );
    }

    private String route(MappedPage page) {
        return firstNonBlank(page.urlPattern(), page.url(), page.pageId());
    }

    private String normalizeRoute(String value) {
        return RouteCanonicalizer.canonicalize(firstNonBlank(value));
    }

    private String normalize(String value) {
        return firstNonBlank(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
