package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LocatorPromotionFilter {

    private static final double MIN_PROMOTED_SCORE = 0.75d;

    public MappedUiKnowledge filterForPersistence(MappedUiKnowledge knowledge) {
        if (knowledge == null) {
            return null;
        }
        List<MappedPage> pages = knowledge.pages().stream()
                .filter(page -> !isBrowserErrorPage(page))
                .map(this::filterPage)
                .toList();
        Set<String> pageIds = pages.stream().map(MappedPage::pageId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition> transitions = knowledge.transitions().stream()
                .filter(transition -> transition.success() && transition.confidenceScore() >= 0.80d)
                .filter(transition -> pageIds.contains(transition.fromPageId()))
                .filter(transition -> transition.toPageId().isBlank() || pageIds.contains(transition.toPageId()))
                .toList();
        PageKnowledgeArtifactBuilder artifactBuilder = new PageKnowledgeArtifactBuilder();
        List<PageKnowledgeGraphNode> graphNodes = new ArrayList<>(artifactBuilder.buildGraphNodes(pages));
        List<PageKnowledgeGraphNode> supplementalNodes = supplementalGraphNodes(knowledge.graphNodes());
        graphNodes.addAll(supplementalNodes);
        Set<String> graphNodeIds = graphNodes.stream()
                .map(PageKnowledgeGraphNode::nodeId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<PageKnowledgeGraphEdge> graphEdges = new ArrayList<>(artifactBuilder.buildGraphEdges(pages, transitions));
        graphEdges.addAll(supplementalGraphEdges(knowledge.graphEdges(), graphNodeIds));
        List<PageKnowledgeVectorDocument> vectorDocuments = new ArrayList<>(artifactBuilder.buildVectorDocuments(pages, transitions));
        vectorDocuments.addAll(supplementalVectorDocuments(knowledge.vectorDocuments()));
        return new MappedUiKnowledge(
                pages,
                transitions,
                graphNodes,
                graphEdges,
                vectorDocuments
        );
    }

    private List<PageKnowledgeGraphNode> supplementalGraphNodes(List<PageKnowledgeGraphNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        return nodes.stream()
                .filter(node -> !isGeneratedMapperNode(node.nodeType()))
                .toList();
    }

    private List<PageKnowledgeGraphEdge> supplementalGraphEdges(
            List<PageKnowledgeGraphEdge> edges,
            Set<String> graphNodeIds
    ) {
        if (edges == null || edges.isEmpty()) {
            return List.of();
        }
        return edges.stream()
                .filter(edge -> graphNodeIds.contains(edge.fromId()) && graphNodeIds.contains(edge.toId()))
                .filter(edge -> !isGeneratedMapperEdge(edge.edgeType()))
                .toList();
    }

    private List<PageKnowledgeVectorDocument> supplementalVectorDocuments(List<PageKnowledgeVectorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .filter(document -> !isGeneratedMapperDocument(document.documentType()))
                .toList();
    }

    private boolean isGeneratedMapperNode(String nodeType) {
        String normalized = nodeType == null ? "" : nodeType.trim();
        return Set.of("Page", "Element", "Locator", "Form", "Field", "Action", "AssertionHint")
                .contains(normalized);
    }

    private boolean isGeneratedMapperEdge(String edgeType) {
        String normalized = edgeType == null ? "" : edgeType.trim();
        return Set.of(
                "PAGE_HAS_ELEMENT",
                "ELEMENT_HAS_LOCATOR",
                "PAGE_HAS_FORM",
                "FORM_HAS_FIELD",
                "PAGE_HAS_ACTION",
                "ELEMENT_SUPPORTS_ACTION",
                "PAGE_TRANSITIONS_TO"
        ).contains(normalized);
    }

    private boolean isGeneratedMapperDocument(String documentType) {
        String normalized = documentType == null ? "" : documentType.trim();
        return Set.of("page-summary", "element-summary", "transition-summary").contains(normalized);
    }

    private MappedPage filterPage(MappedPage page) {
        List<MappedElement> elements = page.elements().stream()
                .map(this::filterElement)
                .toList();
        List<MappedForm> forms = page.forms().stream()
                .map(this::filterForm)
                .toList();
        return new MappedPage(
                page.pageId(),
                page.pageName(),
                page.pageType(),
                page.url(),
                page.urlPattern(),
                page.title(),
                page.sections(),
                elements,
                forms,
                page.actions(),
                page.assertionHints().stream()
                        .filter(hint -> hint.confidenceScore() >= 0.80d && !hint.target().isBlank())
                        .toList(),
                page.stateHints(),
                page.screenshotPath(),
                page.htmlPath(),
                page.canonicalPageType(),
                page.pageIdentity()
        );
    }

    private MappedElement filterElement(MappedElement element) {
        return new MappedElement(
                element.elementId(),
                element.semanticName(),
                element.elementType(),
                element.role(),
                element.text(),
                element.clickable(),
                element.visible(),
                promoted(element.locatorCandidates()),
                element.supportedActions(),
                element.confidenceScore()
        );
    }

    private MappedForm filterForm(MappedForm form) {
        return new MappedForm(
                form.formId(),
                form.formName(),
                form.action(),
                form.fields().stream().map(this::filterField).toList(),
                form.submitActionIds()
        );
    }

    private MappedField filterField(MappedField field) {
        return new MappedField(
                field.fieldId(),
                field.fieldName(),
                field.fieldType(),
                field.label(),
                field.required(),
                field.placeholder(),
                promoted(field.locatorCandidates())
        );
    }

    private List<LocatorCandidate> promoted(List<LocatorCandidate> candidates) {
        return candidates.stream()
                .filter(this::isPromoted)
                .sorted(Comparator.comparingDouble(LocatorCandidate::stabilityScore).reversed())
                .limit(3)
                .toList();
    }

    private boolean isPromoted(LocatorCandidate candidate) {
        if (candidate == null || candidate.stabilityScore() < MIN_PROMOTED_SCORE) {
            return false;
        }
        if (candidate.evidenceType() != LocatorEvidenceType.CONFIRMED_LOCATOR) {
            return false;
        }
        if (!candidate.sameOrigin() || !candidate.uniqueOnPage() || !candidate.stableAcrossRuns()) {
            return false;
        }
        return candidate.risks().stream().noneMatch(this::forbiddenRisk);
    }

    private boolean forbiddenRisk(String risk) {
        String normalized = risk == null ? "" : risk.toLowerCase(Locale.ROOT);
        return normalized.contains("external")
                || normalized.contains("unstable")
                || normalized.contains("not-proven")
                || normalized.contains("low-confidence");
    }

    private boolean isBrowserErrorPage(MappedPage page) {
        if (page == null) {
            return false;
        }
        String text = String.join(" ",
                page.pageName(),
                page.title(),
                page.urlPattern(),
                page.sections().toString(),
                page.elements().stream().map(element -> element.semanticName() + " " + element.text()).toList().toString()
        ).toLowerCase(Locale.ROOT);
        return (text.contains("page can't be found")
                || text.contains("page can t be found")
                || text.contains("this site can't be reached")
                || text.contains("404 not found")
                || text.contains("not found"))
                && text.contains("reload");
    }
}
