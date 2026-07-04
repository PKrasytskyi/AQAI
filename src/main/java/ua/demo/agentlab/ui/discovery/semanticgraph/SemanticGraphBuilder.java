package ua.demo.agentlab.ui.discovery.semanticgraph;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.component.ComponentBoundaryDetector;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeStateTransition;
import ua.demo.agentlab.ui.discovery.runtime.model.SemanticNetworkEvidence;
import ua.demo.agentlab.ui.discovery.semantic.model.ActionCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.BusinessIntentCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticActionModel;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticElementModel;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticPageModel;
import ua.demo.agentlab.ui.discovery.semanticgraph.model.SemanticGraphEdge;
import ua.demo.agentlab.ui.discovery.semanticgraph.model.SemanticGraphModel;
import ua.demo.agentlab.ui.discovery.semanticgraph.model.SemanticGraphNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SemanticGraphBuilder {

    private final ComponentBoundaryDetector componentBoundaryDetector = new ComponentBoundaryDetector();

    public SemanticGraphModel build(
            PageModelBundle pageModelBundle,
            MappedUiKnowledge mappedUiKnowledge,
            SemanticActionModel semanticActionModel,
            RuntimeEvidenceBundle runtimeEvidenceBundle
    ) {
        if (pageModelBundle == null || pageModelBundle.pages().isEmpty()) {
            return SemanticGraphModel.empty("semantic-graph:no-page-model");
        }
        Map<String, MappedPage> mappedPages = mappedPages(mappedUiKnowledge);
        List<SemanticGraphNode> nodes = new ArrayList<>();
        List<SemanticGraphEdge> edges = new ArrayList<>();

        for (PageModel page : pageModelBundle.pages()) {
            String pageNodeId = pageNodeId(page.pageId());
            MappedPage mappedPage = mappedPages.get(page.pageId());
            nodes.add(new SemanticGraphNode(
                    pageNodeId,
                    "SemanticPage",
                    mappedPage == null ? page.pageId() : mappedPage.pageName(),
                    page.pageId(),
                    Map.of(
                            "route", page.route(),
                            "featureGuess", page.featureGuess(),
                            "mappedPageType", mappedPage == null ? "" : mappedPage.pageType()
                    )
            ));
            edges.add(new SemanticGraphEdge(page.pageId(), pageNodeId, "PAGE_HAS_SEMANTIC_MODEL", 0.95d));
        }
        addComponentNodes(pageModelBundle, nodes, edges);

        if (semanticActionModel != null) {
            addSemanticActionNodes(semanticActionModel, nodes, edges);
        }
        if (runtimeEvidenceBundle != null) {
            addRuntimeNodes(runtimeEvidenceBundle, nodes, edges);
        }

        return new SemanticGraphModel(
                nodes,
                edges,
                List.of(
                        "semantic-graph:page-model",
                        "semantic-graph:mapped-knowledge",
                        semanticActionModel == null ? "semantic-action:missing" : "semantic-action:included",
                        runtimeEvidenceBundle == null ? "runtime-evidence:missing" : "runtime-evidence:included"
                )
        );
    }

    private void addComponentNodes(
            PageModelBundle pageModelBundle,
            List<SemanticGraphNode> nodes,
            List<SemanticGraphEdge> edges
    ) {
        var componentModel = componentBoundaryDetector.detect(pageModelBundle);
        for (var page : componentModel.pages()) {
            String pageNodeId = pageNodeId(page.pageId());
            for (SemanticComponentModel component : page.components()) {
                String componentNodeId = page.pageId() + ":semantic-component:" + sanitize(component.name());
                nodes.add(new SemanticGraphNode(
                        componentNodeId,
                        "SemanticComponent",
                        component.name(),
                        page.pageId(),
                        Map.of(
                                "componentType", component.type().name(),
                                "route", page.route(),
                                "elementCount", String.valueOf(component.elementIds().size()),
                                "scopedLocatorCount", String.valueOf(component.locators().size()),
                                "confidence", String.valueOf(component.confidence()),
                                "risks", String.join(",", component.risks())
                        )
                ));
                edges.add(new SemanticGraphEdge(pageNodeId, componentNodeId, "SEMANTIC_PAGE_HAS_COMPONENT", component.confidence()));
                for (String elementId : component.elementIds()) {
                    edges.add(new SemanticGraphEdge(componentNodeId, page.pageId() + ":semantic-element:" + sanitize(elementId),
                            "COMPONENT_OWNS_SEMANTIC_ELEMENT", component.confidence()));
                }
            }
        }
    }

    private void addSemanticActionNodes(
            SemanticActionModel semanticActionModel,
            List<SemanticGraphNode> nodes,
            List<SemanticGraphEdge> edges
    ) {
        for (SemanticPageModel page : semanticActionModel.pages()) {
            String pageNodeId = pageNodeId(page.pageId());
            for (ActionCandidate action : page.pageActionCandidates()) {
                String actionNodeId = page.pageId() + ":semantic-action:" + sanitize(action.action());
                nodes.add(new SemanticGraphNode(
                        actionNodeId,
                        "SemanticActionCandidate",
                        action.action(),
                        page.pageId(),
                        Map.of(
                                "targetElementId", action.targetElementId(),
                                "confidence", String.valueOf(action.confidence()),
                                "evidence", String.join(",", action.evidence())
                        )
                ));
                edges.add(new SemanticGraphEdge(pageNodeId, actionNodeId, "SEMANTIC_PAGE_SUPPORTS_ACTION", action.confidence()));
            }
            for (BusinessIntentCandidate intent : page.pageBusinessIntentCandidates()) {
                addIntentNode(page.pageId(), pageNodeId, intent, nodes, edges);
            }
            for (SemanticElementModel element : page.elements()) {
                String elementNodeId = page.pageId() + ":semantic-element:" + sanitize(element.elementId());
                nodes.add(new SemanticGraphNode(
                        elementNodeId,
                        "SemanticElement",
                        element.name(),
                        page.pageId(),
                        Map.of(
                                "elementType", element.elementType(),
                                "role", element.role(),
                                "confidence", String.valueOf(element.confidence())
                        )
                ));
                edges.add(new SemanticGraphEdge(pageNodeId, elementNodeId, "SEMANTIC_PAGE_HAS_ELEMENT", element.confidence()));
                for (ActionCandidate action : element.actionCandidates()) {
                    String actionNodeId = elementNodeId + ":action:" + sanitize(action.action());
                    nodes.add(new SemanticGraphNode(
                            actionNodeId,
                            "SemanticElementActionCandidate",
                            action.action(),
                            page.pageId(),
                            Map.of(
                                    "targetElementId", action.targetElementId(),
                                    "confidence", String.valueOf(action.confidence()),
                                    "evidence", String.join(",", action.evidence())
                            )
                    ));
                    edges.add(new SemanticGraphEdge(elementNodeId, actionNodeId, "ELEMENT_SUPPORTS_SEMANTIC_ACTION", action.confidence()));
                }
                for (BusinessIntentCandidate intent : element.businessIntentCandidates()) {
                    addIntentNode(page.pageId(), elementNodeId, intent, nodes, edges);
                }
            }
        }
    }

    private void addIntentNode(
            String pageId,
            String ownerNodeId,
            BusinessIntentCandidate intent,
            List<SemanticGraphNode> nodes,
            List<SemanticGraphEdge> edges
    ) {
        String intentNodeId = ownerNodeId + ":intent:" + sanitize(intent.intent());
        nodes.add(new SemanticGraphNode(
                intentNodeId,
                "BusinessIntentCandidate",
                intent.intent(),
                pageId,
                Map.of(
                        "confidence", String.valueOf(intent.confidence()),
                        "needsReview", String.valueOf(intent.needsReview()),
                        "evidence", String.join(",", intent.evidence())
                )
        ));
        edges.add(new SemanticGraphEdge(ownerNodeId, intentNodeId, "HAS_BUSINESS_INTENT_CANDIDATE", intent.confidence()));
    }

    private void addRuntimeNodes(
            RuntimeEvidenceBundle runtimeEvidenceBundle,
            List<SemanticGraphNode> nodes,
            List<SemanticGraphEdge> edges
    ) {
        for (SemanticNetworkEvidence evidence : runtimeEvidenceBundle.semanticNetworkEvidence()) {
            String nodeId = "runtime-network:" + sanitize(evidence.evidenceId());
            nodes.add(new SemanticGraphNode(
                    nodeId,
                    "RuntimeNetworkEvidence",
                    evidence.operation(),
                    evidence.pageId(),
                    Map.of(
                            "method", evidence.method(),
                            "endpoint", evidence.endpoint(),
                            "status", String.valueOf(evidence.status()),
                            "resourceType", evidence.resourceType(),
                            "businessIntent", evidence.businessIntent(),
                            "confidence", String.valueOf(evidence.confidence()),
                            "sourceTrace", evidence.sourceTrace()
                    )
            ));
            if (!evidence.pageId().isBlank()) {
                edges.add(new SemanticGraphEdge(pageNodeId(evidence.pageId()), nodeId, "SEMANTIC_PAGE_OBSERVED_RUNTIME_NETWORK", evidence.confidence()));
            }
        }
        for (RuntimeStateTransition transition : runtimeEvidenceBundle.stateTransitions()) {
            String nodeId = "runtime-transition:" + sanitize(transition.transitionId());
            nodes.add(new SemanticGraphNode(
                    nodeId,
                    "RuntimeStateTransition",
                    transition.transitionType(),
                    transition.fromPageId(),
                    Map.of(
                            "fromPageId", transition.fromPageId(),
                            "toPageId", transition.toPageId(),
                            "toUrl", transition.toUrl(),
                            "routeChanged", String.valueOf(transition.routeChanged()),
                            "sameDocument", String.valueOf(transition.sameDocument()),
                            "trigger", transition.trigger(),
                            "confidence", String.valueOf(transition.confidence())
                    )
            ));
            if (!transition.fromPageId().isBlank()) {
                edges.add(new SemanticGraphEdge(pageNodeId(transition.fromPageId()), nodeId, "SEMANTIC_PAGE_HAS_RUNTIME_TRANSITION", transition.confidence()));
            }
            if (!transition.toPageId().isBlank()) {
                edges.add(new SemanticGraphEdge(nodeId, pageNodeId(transition.toPageId()), "RUNTIME_TRANSITION_TARGETS_PAGE", transition.confidence()));
            }
        }
    }

    private Map<String, MappedPage> mappedPages(MappedUiKnowledge mappedUiKnowledge) {
        Map<String, MappedPage> pages = new LinkedHashMap<>();
        if (mappedUiKnowledge == null) {
            return pages;
        }
        for (MappedPage page : mappedUiKnowledge.pages()) {
            pages.put(page.pageId(), page);
        }
        return pages;
    }

    private String pageNodeId(String pageId) {
        return safe(pageId) + ":semantic-page";
    }

    private String sanitize(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "value" : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
