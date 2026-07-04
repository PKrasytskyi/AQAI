package ua.demo.agentlab.ui.discovery.semantic;

import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.semantic.model.ActionCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.BusinessIntentCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticActionModel;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticElementModel;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticPageModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SemanticActionModelBuilder {

    private final SemanticElementClassifier elementClassifier;
    private final ActionCandidateClassifier actionClassifier;
    private final BusinessIntentResolver businessIntentResolver;

    public SemanticActionModelBuilder() {
        this(new SemanticElementClassifier(), new ActionCandidateClassifier(), new BusinessIntentResolver());
    }

    public SemanticActionModelBuilder(
            SemanticElementClassifier elementClassifier,
            ActionCandidateClassifier actionClassifier,
            BusinessIntentResolver businessIntentResolver
    ) {
        this.elementClassifier = elementClassifier == null ? new SemanticElementClassifier() : elementClassifier;
        this.actionClassifier = actionClassifier == null ? new ActionCandidateClassifier() : actionClassifier;
        this.businessIntentResolver = businessIntentResolver == null ? new BusinessIntentResolver() : businessIntentResolver;
    }

    public SemanticActionModel build(PageModelBundle pageModelBundle, MappedUiKnowledge mappedUiKnowledge) {
        if (pageModelBundle == null || pageModelBundle.pages().isEmpty()) {
            return SemanticActionModel.empty("semantic-action:no-page-models");
        }
        List<SemanticPageModel> pages = pageModelBundle.pages().stream()
                .map(page -> buildPage(page, matchingMappedPage(page, mappedUiKnowledge).orElse(null)))
                .toList();
        return new SemanticActionModel(pages, List.of("semantic-action:page-model-bundle"));
    }

    public SemanticPageModel buildForTarget(PageModelBundle pageModelBundle, MappedUiKnowledge mappedUiKnowledge, MappedPage targetPage) {
        if (pageModelBundle == null || targetPage == null) {
            return null;
        }
        return pageModelBundle.pages().stream()
                .filter(page -> pageMatchesTarget(page, targetPage))
                .findFirst()
                .map(page -> buildPage(page, targetPage))
                .orElse(null);
    }

    private SemanticPageModel buildPage(PageModel page, MappedPage mappedPage) {
        List<SemanticElementModel> elements = page.elements().stream()
                .map(element -> buildElement(page, element))
                .toList();
        List<ActionCandidate> pageActions = pageActions(elements);
        List<BusinessIntentCandidate> pageIntents = businessIntentResolver.resolveForPage(page, elements);
        double confidence = Math.max(
                elements.stream().mapToDouble(SemanticElementModel::confidence).average().orElse(0.0d),
                pageIntents.stream().mapToDouble(BusinessIntentCandidate::confidence).average().orElse(0.0d)
        );
        return new SemanticPageModel(
                page.pageId(),
                mappedPage == null ? "" : mappedPage.pageName(),
                page.route(),
                mappedPage == null ? page.featureGuess() : mappedPage.pageType(),
                elements,
                pageActions,
                pageIntents,
                confidence
        );
    }

    private SemanticElementModel buildElement(PageModel page, PageElementModel element) {
        String semanticType = elementClassifier.classify(element);
        List<ActionCandidate> actions = actionClassifier.classify(element, semanticType);
        List<BusinessIntentCandidate> intents = businessIntentResolver.resolveForElement(page, element, semanticType, actions);
        double confidence = Math.max(
                element.confidenceScore(),
                Math.max(
                        actions.stream().mapToDouble(ActionCandidate::confidence).max().orElse(0.0d),
                        intents.stream().mapToDouble(BusinessIntentCandidate::confidence).max().orElse(0.0d)
                )
        );
        return new SemanticElementModel(
                element.elementId(),
                elementClassifier.semanticName(element),
                semanticType,
                element.role(),
                element.text(),
                element.locatorCandidates(),
                actions,
                intents,
                confidence
        );
    }

    private List<ActionCandidate> pageActions(List<SemanticElementModel> elements) {
        Map<String, ActionCandidate> candidates = new LinkedHashMap<>();
        for (SemanticElementModel element : elements) {
            for (ActionCandidate action : element.actionCandidates()) {
                String key = action.action() + "|" + action.targetElementId();
                candidates.putIfAbsent(key, action);
            }
        }
        return new ArrayList<>(candidates.values()).stream()
                .sorted(Comparator.comparingDouble(ActionCandidate::confidence).reversed())
                .limit(24)
                .toList();
    }

    private Optional<MappedPage> matchingMappedPage(PageModel page, MappedUiKnowledge mappedUiKnowledge) {
        if (mappedUiKnowledge == null || mappedUiKnowledge.pages().isEmpty()) {
            return Optional.empty();
        }
        return mappedUiKnowledge.pages().stream()
                .filter(mappedPage -> pageMatchesMapped(page, mappedPage))
                .findFirst();
    }

    private boolean pageMatchesTarget(PageModel page, MappedPage targetPage) {
        return pageMatchesMapped(page, targetPage);
    }

    private boolean pageMatchesMapped(PageModel page, MappedPage mappedPage) {
        if (page == null || mappedPage == null) {
            return false;
        }
        if (!page.pageId().isBlank() && page.pageId().equalsIgnoreCase(mappedPage.pageId())) {
            return true;
        }
        return RouteCanonicalizer.routeEqualsOrSuffix(page.route(), mappedPage.urlPattern())
                || RouteCanonicalizer.routeEqualsOrSuffix(page.route(), mappedPage.url())
                || RouteCanonicalizer.routeEqualsOrSuffix(page.url(), mappedPage.urlPattern())
                || RouteCanonicalizer.routeEqualsOrSuffix(page.url(), mappedPage.url());
    }
}
