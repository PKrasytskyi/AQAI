package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;

import java.util.List;
import java.util.Locale;

final class ComponentEvidenceMatcher {

    List<String> ids(ComponentDetectionContext context, String... terms) {
        return context.elementsById().values().stream().filter(this::usable)
                .filter(element -> contains(evidence(element), terms)).map(PageElementModel::elementId).toList();
    }

    boolean usable(PageElementModel element) {
        return element != null && element.visible() && (!element.locatorCandidates().isEmpty()
                || !element.text().isBlank() || !element.role().isBlank());
    }

    String evidence(PageElementModel element) {
        return String.join(" ", element.elementId(), element.technicalType(), element.semanticType(), element.tag(),
                element.text(), element.id(), element.name(), element.placeholder(), element.ariaLabel(), element.role(),
                element.href(), element.cssClass(), element.attributes().toString()).toLowerCase(Locale.ROOT);
    }

    private boolean contains(String text, String... terms) {
        for (String term : terms) if (text.contains(term.toLowerCase(Locale.ROOT))) return true;
        return false;
    }
}
