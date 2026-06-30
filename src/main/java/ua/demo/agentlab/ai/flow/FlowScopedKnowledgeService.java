package ua.demo.agentlab.ai.flow;

import ua.demo.agentlab.ai.context.CanonicalInteractionLayer;
import ua.demo.agentlab.ai.context.CanonicalUiInteraction;
import ua.demo.agentlab.ai.context.CanonicalUiInteractionModel;
import ua.demo.agentlab.ai.context.UiKnowledgeGraphMatch;
import ua.demo.agentlab.ai.context.UiKnowledgeRetrievalContext;
import ua.demo.agentlab.ai.context.UiKnowledgeRetrievalRequest;
import ua.demo.agentlab.ai.context.UiKnowledgeRetrievalService;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.discovery.mapping.model.AssertionHint;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedSection;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.net.URI;

public class FlowScopedKnowledgeService {

    private final BusinessFlowResolver businessFlowResolver;
    private final CanonicalInteractionLayer canonicalInteractionLayer;
    private final UiKnowledgeRetrievalService retrievalService;

    public FlowScopedKnowledgeService(
            BusinessFlowResolver businessFlowResolver,
            CanonicalInteractionLayer canonicalInteractionLayer,
            UiKnowledgeRetrievalService retrievalService
    ) {
        if (businessFlowResolver == null) {
            throw new IllegalArgumentException("businessFlowResolver cannot be null");
        }
        if (canonicalInteractionLayer == null) {
            throw new IllegalArgumentException("canonicalInteractionLayer cannot be null");
        }
        this.businessFlowResolver = businessFlowResolver;
        this.canonicalInteractionLayer = canonicalInteractionLayer;
        this.retrievalService = retrievalService;
    }

    public FlowScopedKnowledgePackage scope(FlowScopedKnowledgeInput input) {
        if (input == null || input.mappedUiKnowledge() == null) {
            throw new IllegalArgumentException("mapped UI knowledge is required");
        }

        MappedUiKnowledge rawKnowledge = input.mappedUiKnowledge();
        CanonicalUiInteractionModel rawCanonicalModel = canonicalInteractionLayer.build(rawKnowledge);
        BusinessFlowContext flowContext = businessFlowResolver.resolve(input);

        List<String> selectedPageIds = selectPageIds(rawKnowledge, flowContext);
        List<String> selectedGraphNodeIds = selectGraphNodeIds(rawKnowledge, selectedPageIds, flowContext);

        UiKnowledgeRetrievalContext rawRetrieval = retrieveFlowContext(input, rawCanonicalModel, selectedPageIds, flowContext);
        UiKnowledgeRetrievalContext curatedRetrieval = filterRetrievalContext(rawRetrieval, selectedPageIds, flowContext);
        CanonicalUiInteractionModel curatedCanonicalModel = filterCanonicalInteractions(rawCanonicalModel, selectedPageIds, flowContext);
        MappedUiKnowledge curatedKnowledge = filterMappedKnowledge(rawKnowledge, curatedCanonicalModel, curatedRetrieval, selectedPageIds, flowContext);

        return new FlowScopedKnowledgePackage(
                flowContext,
                selectedPageIds,
                selectedGraphNodeIds,
                curatedKnowledge,
                curatedCanonicalModel,
                curatedRetrieval,
                resolveExcludedSignals(rawCanonicalModel, curatedCanonicalModel)
        );
    }

    private UiKnowledgeRetrievalContext retrieveFlowContext(
            FlowScopedKnowledgeInput input,
            CanonicalUiInteractionModel rawCanonicalModel,
            List<String> selectedPageIds,
            BusinessFlowContext flowContext
    ) {
        if (retrievalService == null) {
            return UiKnowledgeRetrievalContext.empty("Flow-scoped retrieval service is not configured");
        }
        try {
            return retrievalService.retrieve(
                    new UiKnowledgeRetrievalRequest(
                            input.projectProfile(),
                            input.normalizedRequirementBundle(),
                            input.testPlan(),
                            null,
                            input.canonicalTestCaseBundle(),
                            input.mappedUiKnowledge(),
                            input.knowledgeRunMetadata(),
                            rawCanonicalModel,
                            selectedPageIds,
                            flowContext.requiredTerms(),
                            buildFlowQuery(input, flowContext)
                    )
            );
        } catch (Exception exception) {
            return new UiKnowledgeRetrievalContext(
                    buildFlowQuery(input, flowContext),
                    flowContext.requiredTerms(),
                    List.of(),
                    List.of(),
                    "FAILED",
                    "FAILED",
                    List.of("Flow-scoped retrieval fallback applied: " + exception.getMessage())
            );
        }
    }

    private String buildFlowQuery(FlowScopedKnowledgeInput input, BusinessFlowContext flowContext) {
        StringBuilder builder = new StringBuilder();
        if (input.projectProfile() != null) {
            builder.append(input.projectProfile().projectName()).append(' ');
            builder.append(input.projectProfile().baseUrl()).append(' ');
        }
        builder.append(flowContext.objective()).append(' ');
        flowContext.targetOperations().forEach(operation -> builder.append(operation).append(' '));
        flowContext.requiredTerms().forEach(term -> builder.append(term).append(' '));
        return builder.toString().trim();
    }

    private List<String> selectPageIds(MappedUiKnowledge knowledge, BusinessFlowContext flowContext) {
        Set<String> selected = new LinkedHashSet<>();
        for (MappedPage page : knowledge.pages()) {
            if (matchesExplicitFlowTarget(page, flowContext)) {
                selected.add(page.pageId());
            }
        }

        Map<String, Double> scores = new LinkedHashMap<>();
        for (MappedPage page : knowledge.pages()) {
            double score = scorePage(page, flowContext);
            if (score > 0.0d) {
                scores.put(page.pageId(), score);
            }
        }
        scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .filter(pageId -> !selected.contains(pageId))
                .limit(Math.max(0, 5 - selected.size()))
                .forEach(selected::add);

        List<String> selectedPageIds = List.copyOf(selected);
        if (!selectedPageIds.isEmpty()) {
            return selectedPageIds;
        }

        List<String> scoredFallback = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
        if (!scoredFallback.isEmpty()) {
            return scoredFallback;
        }
        return knowledge.pages().stream().limit(2).map(MappedPage::pageId).toList();
    }

    private boolean matchesExplicitFlowTarget(MappedPage page, BusinessFlowContext flowContext) {
        for (String pageName : flowContext.targetPageNames()) {
            if (PageReferenceMatcher.matches(page, pageName)) {
                return true;
            }
        }
        for (String route : flowContext.targetRoutes()) {
            if (sameConcreteRoute(page.urlPattern(), route) || sameConcreteRoute(page.url(), route)) {
                return true;
            }
        }
        return false;
    }

    private List<String> selectGraphNodeIds(
            MappedUiKnowledge knowledge,
            List<String> selectedPageIds,
            BusinessFlowContext flowContext
    ) {
        Set<String> nodeIds = new LinkedHashSet<>();
        for (PageKnowledgeGraphNode node : knowledge.graphNodes()) {
            if (selectedPageIds.contains(node.pageId()) && matchesBusinessFlow(node.name(), flowContext, false)) {
                nodeIds.add(node.nodeId());
            }
        }
        return List.copyOf(nodeIds);
    }

    private double scorePage(MappedPage page, BusinessFlowContext flowContext) {
        String pageText = normalize(page.pageName() + " " + page.pageType() + " " + page.urlPattern() + " " + page.url() + " " + page.title());
        double score = 0.0d;
        for (String route : flowContext.targetRoutes()) {
            if (!route.isBlank() && pageText.contains(normalize(route))) {
                score += 4.0d;
            }
        }
        for (String pageName : flowContext.targetPageNames()) {
            if (!pageName.isBlank() && pageText.contains(normalize(pageName.replace("Page", "")))) {
                score += 3.0d;
            }
        }
        for (String term : flowContext.requiredTerms()) {
            if (pageText.contains(term)) {
                score += 0.8d;
            }
        }
        for (PageCapability capability : flowContext.targetCapabilities()) {
            if (page.canonicalPageType() == capability.canonicalPageType()
                    || page.pageIdentity() != null
                    && page.pageIdentity().canonicalPageType() == capability.canonicalPageType()) {
                score += 4.0d;
            }
        }
        score += countRelevantElements(page.elements(), flowContext) * 0.35d;
        score += countRelevantActions(page.actions(), page.elements(), flowContext) * 0.65d;
        for (String excluded : flowContext.excludedTerms()) {
            if (pageText.contains(excluded)) {
                score -= 1.25d;
            }
        }
        return score;
    }

    private int countRelevantElements(List<MappedElement> elements, BusinessFlowContext flowContext) {
        int count = 0;
        for (MappedElement element : elements) {
            if (matchesBusinessFlow(elementText(element), flowContext, true)) {
                count++;
            }
        }
        return count;
    }

    private int countRelevantActions(
            List<MappedAction> actions,
            List<MappedElement> elements,
            BusinessFlowContext flowContext
    ) {
        Map<String, MappedElement> elementsById = mapElementsById(elements);
        int count = 0;
        for (MappedAction action : actions) {
            MappedElement sourceElement = elementsById.get(action.sourceElementId());
            if (isRelevantAction(action, sourceElement, flowContext, false)) {
                count++;
            }
        }
        return count;
    }

    private CanonicalUiInteractionModel filterCanonicalInteractions(
            CanonicalUiInteractionModel rawCanonicalModel,
            List<String> selectedPageIds,
            BusinessFlowContext flowContext
    ) {
        List<CanonicalUiInteraction> interactions = rawCanonicalModel.interactions().stream()
                .filter(interaction -> isRelevantCanonicalInteraction(interaction, selectedPageIds, flowContext, false))
                .limit(16)
                .toList();
        if (interactions.isEmpty()) {
            interactions = rawCanonicalModel.interactions().stream()
                    .filter(interaction -> selectedPageIds.contains(interaction.pageId()))
                    .limit(8)
                    .toList();
        }
        return new CanonicalUiInteractionModel(interactions);
    }

    private UiKnowledgeRetrievalContext filterRetrievalContext(
            UiKnowledgeRetrievalContext rawRetrieval,
            List<String> selectedPageIds,
            BusinessFlowContext flowContext
    ) {
        List<RetrievedChunk> vectorMatches = rawRetrieval.vectorMatches().stream()
                .filter(match -> isRelevantRetrievedChunk(match, selectedPageIds, flowContext))
                .limit(4)
                .toList();
        List<UiKnowledgeGraphMatch> graphMatches = rawRetrieval.graphMatches().stream()
                .filter(match -> isRelevantGraphMatch(match, selectedPageIds, flowContext))
                .limit(6)
                .toList();
        return new UiKnowledgeRetrievalContext(
                rawRetrieval.query(),
                rawRetrieval.queryTerms(),
                vectorMatches,
                graphMatches,
                rawRetrieval.vectorSource(),
                rawRetrieval.graphSource(),
                rawRetrieval.notes()
        );
    }

    private MappedUiKnowledge filterMappedKnowledge(
            MappedUiKnowledge rawKnowledge,
            CanonicalUiInteractionModel curatedCanonicalModel,
            UiKnowledgeRetrievalContext curatedRetrieval,
            List<String> selectedPageIds,
            BusinessFlowContext flowContext
    ) {
        Set<String> requiredElementIds = curatedCanonicalModel.interactions().stream()
                .map(CanonicalUiInteraction::sourceElementId)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<MappedPage> filteredPages = new ArrayList<>();
        for (MappedPage page : rawKnowledge.pages()) {
            if (!selectedPageIds.contains(page.pageId())) {
                continue;
            }
            filteredPages.add(filterPage(page, requiredElementIds, flowContext));
        }

        Set<String> filteredPageIds = filteredPages.stream()
                .map(MappedPage::pageId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> filteredActionIds = filteredPages.stream()
                .flatMap(page -> page.actions().stream())
                .map(MappedAction::actionId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> filteredElementIds = filteredPages.stream()
                .flatMap(page -> page.elements().stream())
                .map(MappedElement::elementId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<MappedTransition> transitions = rawKnowledge.transitions().stream()
                .filter(transition -> filteredPageIds.contains(transition.fromPageId())
                        || filteredPageIds.contains(transition.toPageId()))
                .limit(12)
                .toList();

        List<PageKnowledgeGraphNode> graphNodes = rawKnowledge.graphNodes().stream()
                .filter(node -> filteredPageIds.contains(node.pageId()) || matchesBusinessFlow(node.name(), flowContext, false))
                .limit(30)
                .toList();

        Set<String> graphNodeIds = graphNodes.stream()
                .map(PageKnowledgeGraphNode::nodeId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<PageKnowledgeGraphEdge> graphEdges = rawKnowledge.graphEdges().stream()
                .filter(edge -> graphNodeIds.contains(edge.fromId()) && graphNodeIds.contains(edge.toId()))
                .limit(60)
                .toList();

        List<PageKnowledgeVectorDocument> vectorDocuments = rawKnowledge.vectorDocuments().stream()
                .filter(document -> filteredPageIds.contains(document.sourcePageId())
                        && matchesBusinessFlow(document.text() + " " + String.join(" ", document.keywords()), flowContext, false))
                .limit(12)
                .toList();
        if (vectorDocuments.isEmpty()) {
            vectorDocuments = curatedRetrieval.vectorMatches().stream()
                    .map(match -> new PageKnowledgeVectorDocument(
                            match.chunkId(),
                            match.metadata() == null ? "rag-chunk" : match.metadata().artifactType().name(),
                            resolveSourcePageId(filteredPageIds, match),
                            match.chunkId(),
                            match.text(),
                            match.metadata() == null ? List.of() : match.metadata().tags()
                    ))
                    .limit(8)
                    .toList();
        }

        return new MappedUiKnowledge(filteredPages, transitions, graphNodes, graphEdges, vectorDocuments);
    }

    private MappedPage filterPage(
            MappedPage page,
            Set<String> requiredElementIds,
            BusinessFlowContext flowContext
    ) {
        Map<String, MappedElement> elementsById = mapElementsById(page.elements());

        List<MappedAction> actions = page.actions().stream()
                .filter(action -> isRelevantAction(action, elementsById.get(action.sourceElementId()), flowContext, true))
                .limit(10)
                .toList();

        Set<String> actionElementIds = actions.stream()
                .map(MappedAction::sourceElementId)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        actionElementIds.addAll(requiredElementIds);

        List<MappedElement> elements = page.elements().stream()
                .filter(element -> actionElementIds.contains(element.elementId())
                        || isRelevantElement(element, flowContext, true))
                .limit(12)
                .toList();

        Set<String> elementIds = elements.stream()
                .map(MappedElement::elementId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<MappedSection> sections = page.sections().stream()
                .map(section -> new MappedSection(
                        section.sectionId(),
                        section.sectionName(),
                        section.sectionType(),
                        section.elementIds().stream().filter(elementIds::contains).toList()
                ))
                .filter(section -> !section.elementIds().isEmpty())
                .limit(6)
                .toList();

        Set<String> actionIds = actions.stream()
                .map(MappedAction::actionId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<MappedForm> forms = page.forms().stream()
                .filter(form -> matchesBusinessFlow(form.formName() + " " + form.action(), flowContext, true)
                        || form.submitActionIds().stream().anyMatch(actionIds::contains))
                .limit(4)
                .toList();

        List<AssertionHint> assertionHints = page.assertionHints().stream()
                .filter(hint -> isRelevantAssertionHint(hint, flowContext))
                .limit(8)
                .toList();

        return new MappedPage(
                page.pageId(),
                page.pageName(),
                page.pageType(),
                page.url(),
                page.urlPattern(),
                page.title(),
                sections,
                elements,
                forms,
                actions,
                assertionHints,
                page.stateHints(),
                page.screenshotPath(),
                page.htmlPath(),
                page.canonicalPageType(),
                page.pageIdentity()
        );
    }

    private boolean isRelevantCanonicalInteraction(
            CanonicalUiInteraction interaction,
            List<String> selectedPageIds,
            BusinessFlowContext flowContext,
            boolean allowNoisyFallback
    ) {
        if (!selectedPageIds.contains(interaction.pageId()) && !selectedPageIds.contains(interaction.targetPageId())) {
            return false;
        }
        String text = normalize(interaction.pageName() + " " + interaction.canonicalName() + " "
                + interaction.interactionType() + " " + interaction.sourceElementName() + " "
                + interaction.targetPageId() + " " + interaction.targetRoute() + " "
                + interaction.subjectType() + " " + interaction.targetType() + " "
                + String.join(" ", interaction.domainHints()) + " "
                + String.join(" ", interaction.keywords()));
        if (matchesExcluded(text, flowContext) && !allowNoisyFallback) {
            return false;
        }
        return matchesBusinessFlow(text, flowContext, allowNoisyFallback)
                || flowContext.targetOperations().contains(interaction.interactionType())
                || flowContext.targetOperations().contains(interaction.canonicalName())
                || flowContext.requiredTerms().stream().anyMatch(term -> normalize(interaction.domainHints().toString()).contains(term));
    }

    private boolean isRelevantAction(
            MappedAction action,
            MappedElement sourceElement,
            BusinessFlowContext flowContext,
            boolean allowNoisyFallback
    ) {
        String text = normalize(action.actionName() + " " + action.actionType() + " "
                + action.description() + " " + action.targetPageId() + " "
                + (sourceElement == null ? "" : elementText(sourceElement)));
        if (matchesExcluded(text, flowContext) && !allowNoisyFallback) {
            return false;
        }
        return matchesBusinessFlow(text, flowContext, allowNoisyFallback);
    }

    private boolean isRelevantElement(MappedElement element, BusinessFlowContext flowContext, boolean allowNoisyFallback) {
        String text = elementText(element);
        if (matchesExcluded(text, flowContext) && !allowNoisyFallback) {
            return false;
        }
        return matchesBusinessFlow(text, flowContext, allowNoisyFallback);
    }

    private boolean isRelevantAssertionHint(AssertionHint hint, BusinessFlowContext flowContext) {
        String text = normalize(hint.hintType() + " " + hint.target() + " " + hint.description());
        return matchesBusinessFlow(text, flowContext, true);
    }

    private boolean isRelevantRetrievedChunk(
            RetrievedChunk chunk,
            List<String> selectedPageIds,
            BusinessFlowContext flowContext
    ) {
        String text = normalize(chunk.text());
        String tags = chunk.metadata() == null ? "" : normalize(String.join(" ", chunk.metadata().tags()));
        boolean pageMatch = chunk.metadata() != null
                && chunk.metadata().tags().stream().map(this::normalize).anyMatch(selectedPageIds::contains);
        return pageMatch || (matchesBusinessFlow(text + " " + tags, flowContext, false) && !matchesExcluded(text + " " + tags, flowContext));
    }

    private boolean isRelevantGraphMatch(
            UiKnowledgeGraphMatch match,
            List<String> selectedPageIds,
            BusinessFlowContext flowContext
    ) {
        String text = normalize(match.name() + " " + match.nodeType() + " " + match.pageId());
        return selectedPageIds.contains(match.pageId()) && matchesBusinessFlow(text, flowContext, false);
    }

    private boolean matchesBusinessFlow(String text, BusinessFlowContext flowContext, boolean allowNoisyFallback) {
        String normalized = normalize(text);
        int hits = 0;
        for (String term : flowContext.requiredTerms()) {
            if (normalized.contains(term)) {
                hits++;
            }
        }
        for (String operation : flowContext.targetOperations()) {
            if (normalized.contains(normalize(operation))) {
                hits += 2;
            }
        }
        for (PageCapability capability : flowContext.targetCapabilities()) {
            String capabilityText = normalize(capability.name() + " "
                    + capability.defaultPageName() + " "
                    + capability.canonicalPageType().mappedType() + " "
                    + capability.canonicalPageType().defaultAlias());
            if (normalized.contains(capabilityText)
                    || normalized.contains(normalize(capability.canonicalPageType().mappedType()))
                    || normalized.contains(normalize(capability.canonicalPageType().defaultAlias()))) {
                hits += 2;
            }
        }
        return hits > 0 || allowNoisyFallback && flowContext.targetCapabilities().isEmpty();
    }

    private boolean matchesExcluded(String text, BusinessFlowContext flowContext) {
        String normalized = normalize(text);
        for (String excluded : flowContext.excludedTerms()) {
            if (normalized.contains(excluded)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, MappedElement> mapElementsById(List<MappedElement> elements) {
        Map<String, MappedElement> elementsById = new LinkedHashMap<>();
        for (MappedElement element : elements) {
            elementsById.put(element.elementId(), element);
        }
        return elementsById;
    }

    private String elementText(MappedElement element) {
        String locatorText = element.locatorCandidates().stream()
                .map(LocatorCandidate::value)
                .reduce("", (left, right) -> left + " " + right);
        return normalize(element.semanticName() + " " + element.elementType() + " "
                + element.role() + " " + element.text() + " " + locatorText + " "
                + String.join(" ", element.supportedActions()));
    }

    private List<String> resolveExcludedSignals(
            CanonicalUiInteractionModel rawCanonicalModel,
            CanonicalUiInteractionModel curatedCanonicalModel
    ) {
        Set<String> selectedIds = curatedCanonicalModel.interactions().stream()
                .map(CanonicalUiInteraction::interactionId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return rawCanonicalModel.interactions().stream()
                .filter(interaction -> !selectedIds.contains(interaction.interactionId()))
                .map(interaction -> interaction.pageName() + ":" + interaction.canonicalName())
                .limit(20)
                .toList();
    }

    private String resolveSourcePageId(Set<String> selectedPageIds, RetrievedChunk match) {
        if (match.metadata() != null) {
            for (String tag : match.metadata().tags()) {
                if (selectedPageIds.contains(tag)) {
                    return tag;
                }
            }
        }
        return selectedPageIds.stream().findFirst().orElse("");
    }

    private boolean sameConcreteRoute(String left, String right) {
        String normalizedLeft = normalizeConcreteRoute(left);
        String normalizedRight = normalizeConcreteRoute(right);
        return !normalizedLeft.isBlank() && normalizedLeft.equals(normalizedRight);
    }

    private String normalizeConcreteRoute(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String route = value.trim();
        try {
            URI uri = URI.create(route);
            if (uri.getPath() != null && !uri.getPath().isBlank()) {
                route = uri.getPath();
            }
        } catch (Exception ignored) {
            // Keep the original text and normalize it below.
        }
        int queryIndex = route.indexOf('?');
        if (queryIndex >= 0) {
            route = route.substring(0, queryIndex);
        }
        int hashIndex = route.indexOf('#');
        if (hashIndex >= 0) {
            route = route.substring(0, hashIndex);
        }
        if (!route.startsWith("/")) {
            route = "/" + route;
        }
        while (route.length() > 1 && route.endsWith("/")) {
            route = route.substring(0, route.length() - 1);
        }
        return route.toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
