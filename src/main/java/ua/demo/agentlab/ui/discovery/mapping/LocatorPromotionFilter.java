package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
        return new MappedUiKnowledge(
                pages,
                knowledge.transitions(),
                new PageKnowledgeArtifactBuilder().buildGraphNodes(pages),
                new PageKnowledgeArtifactBuilder().buildGraphEdges(pages, knowledge.transitions()),
                new PageKnowledgeArtifactBuilder().buildVectorDocuments(pages, knowledge.transitions())
        );
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
                page.assertionHints(),
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
