package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.mapping.model.AssertionHint;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PageKnowledgeArtifactBuilder {

    public List<PageKnowledgeGraphNode> buildGraphNodes(List<MappedPage> pages) {
        List<PageKnowledgeGraphNode> nodes = new ArrayList<>();
        for (MappedPage page : pages) {
            nodes.add(new PageKnowledgeGraphNode(page.pageId(), "Page", page.pageName(), page.pageId(), Map.of(
                    "type", page.pageType(),
                    "route", page.urlPattern()
            )));
            for (MappedElement element : page.elements()) {
                nodes.add(new PageKnowledgeGraphNode(element.elementId(), "Element", element.semanticName(), page.pageId(), Map.of(
                        "type", element.elementType(),
                        "text", element.text()
                )));
                for (LocatorCandidate locator : element.locatorCandidates()) {
                    nodes.add(new PageKnowledgeGraphNode(
                            element.elementId() + ":locator:" + sanitize(locator.strategy().wireName() + "-" + locator.value()),
                            "Locator",
                            locator.strategy().wireName(),
                            page.pageId(),
                            Map.of(
                                    "value", locator.value(),
                                    "stabilityScore", String.valueOf(locator.stabilityScore()),
                                    "sameOrigin", String.valueOf(locator.sameOrigin()),
                                    "originHost", locator.originHost(),
                                    "risks", String.join(",", locator.risks())
                            )
                    ));
                }
            }
            for (MappedForm form : page.forms()) {
                nodes.add(new PageKnowledgeGraphNode(form.formId(), "Form", form.formName(), page.pageId(), Map.of(
                        "action", form.action()
                )));
                for (MappedField field : form.fields()) {
                    nodes.add(new PageKnowledgeGraphNode(field.fieldId(), "Field", field.fieldName(), page.pageId(), Map.of(
                            "type", field.fieldType(),
                            "required", String.valueOf(field.required())
                    )));
                }
            }
            for (MappedAction action : page.actions()) {
                nodes.add(new PageKnowledgeGraphNode(action.actionId(), "Action", action.actionName(), page.pageId(), Map.of(
                        "type", action.actionType()
                )));
            }
            for (AssertionHint assertionHint : page.assertionHints()) {
                nodes.add(new PageKnowledgeGraphNode(
                        page.pageId() + ":assertion:" + sanitize(assertionHint.hintType() + "-" + assertionHint.target()),
                        "AssertionHint",
                        assertionHint.hintType(),
                        page.pageId(),
                        Map.of("target", assertionHint.target())
                ));
            }
        }
        return nodes;
    }

    public List<PageKnowledgeGraphEdge> buildGraphEdges(List<MappedPage> pages, List<MappedTransition> transitions) {
        List<PageKnowledgeGraphEdge> edges = new ArrayList<>();
        for (MappedPage page : pages) {
            for (MappedElement element : page.elements()) {
                edges.add(new PageKnowledgeGraphEdge(page.pageId(), element.elementId(), "PAGE_HAS_ELEMENT"));
                for (LocatorCandidate locator : element.locatorCandidates()) {
                    edges.add(new PageKnowledgeGraphEdge(
                            element.elementId(),
                            element.elementId() + ":locator:" + sanitize(locator.strategy().wireName() + "-" + locator.value()),
                            "ELEMENT_HAS_LOCATOR"
                    ));
                }
            }
            for (MappedForm form : page.forms()) {
                edges.add(new PageKnowledgeGraphEdge(page.pageId(), form.formId(), "PAGE_HAS_FORM"));
                for (MappedField field : form.fields()) {
                    edges.add(new PageKnowledgeGraphEdge(form.formId(), field.fieldId(), "FORM_HAS_FIELD"));
                }
            }
            for (MappedAction action : page.actions()) {
                edges.add(new PageKnowledgeGraphEdge(page.pageId(), action.actionId(), "PAGE_HAS_ACTION"));
                if (!action.sourceElementId().isBlank()) {
                    edges.add(new PageKnowledgeGraphEdge(action.sourceElementId(), action.actionId(), "ELEMENT_SUPPORTS_ACTION"));
                }
            }
        }
        for (MappedTransition transition : transitions) {
            if (transition.success()) {
                edges.add(new PageKnowledgeGraphEdge(transition.fromPageId(), transition.toPageId(), "PAGE_TRANSITIONS_TO"));
            }
        }
        return edges;
    }

    public List<PageKnowledgeVectorDocument> buildVectorDocuments(List<MappedPage> pages, List<MappedTransition> transitions) {
        List<PageKnowledgeVectorDocument> documents = new ArrayList<>();
        for (MappedPage page : pages) {
            documents.add(new PageKnowledgeVectorDocument(
                    page.pageId() + ":summary",
                    "page-summary",
                    page.pageId(),
                    page.pageId(),
                    page.pageName() + " route=" + page.urlPattern()
                            + " type=" + page.pageType()
                            + " actions=" + page.actions().stream().map(MappedAction::actionName).collect(Collectors.joining(", "))
                            + " assertions=" + page.assertionHints().stream().map(AssertionHint::hintType).collect(Collectors.joining(", ")),
                    keywords(page.pageName(), page.pageType(), page.urlPattern())
            ));
            for (MappedElement element : page.elements()) {
                documents.add(new PageKnowledgeVectorDocument(
                        element.elementId() + ":summary",
                        "element-summary",
                        page.pageId(),
                        element.elementId(),
                        "Element " + element.semanticName() + " type=" + element.elementType()
                                + " text=" + element.text()
                                + " actions=" + String.join(", ", element.supportedActions()),
                        keywords(page.pageName(), element.semanticName(), element.elementType(), element.text())
                ));
            }
        }
        for (MappedTransition transition : transitions) {
            documents.add(new PageKnowledgeVectorDocument(
                    transition.transitionId() + ":summary",
                    "transition-summary",
                    transition.fromPageId(),
                    transition.transitionId(),
                    "Transition from " + transition.fromPageId() + " to " + transition.toPageId()
                            + " actionType=" + transition.actionType(),
                    keywords(transition.fromPageId(), transition.toPageId(), transition.actionType())
            ));
        }
        return documents;
    }

    private List<String> keywords(String... parts) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            for (String token : part.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (token.length() >= 3) {
                    keywords.add(token);
                }
            }
        }
        return List.copyOf(keywords);
    }

    private String sanitize(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "value" : normalized;
    }
}
