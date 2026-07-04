package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageActionModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.util.Comparator;
import java.util.List;

public class PageModelElementMapper {

    private final LocatorQualityEvaluator locatorQualityEvaluator;

    public PageModelElementMapper() {
        this(new LocatorQualityEvaluator());
    }

    public PageModelElementMapper(LocatorQualityEvaluator locatorQualityEvaluator) {
        this.locatorQualityEvaluator = locatorQualityEvaluator == null ? new LocatorQualityEvaluator() : locatorQualityEvaluator;
    }

    public MappedElement map(String pageUrl, PageElementModel element) {
        return new MappedElement(
                element.elementId(),
                semanticName(element),
                firstNonBlank(element.semanticType(), element.technicalType(), element.tag()),
                element.role(),
                element.text(),
                isClickable(element),
                element.visible(),
                mapLocators(pageUrl, element, element.locatorCandidates()),
                element.actions().stream().map(PageActionModel::actionType).distinct().toList(),
                element.confidenceScore()
        );
    }

    public List<MappedAction> mapActions(String pageId, PageElementModel element) {
        return element.actions().stream()
                .map(action -> new MappedAction(
                        action.actionId(),
                        action.actionName(),
                        action.actionType(),
                        element.elementId(),
                        "",
                        action.description().isBlank()
                                ? "Derived from PageModel element " + semanticName(element) + " on " + pageId
                                : action.description(),
                        action.confidenceScore()
                ))
                .toList();
    }

    private List<LocatorCandidate> mapLocators(String pageUrl, PageElementModel element, List<PageLocatorModel> locators) {
        if (element == null || !element.visible()) {
            return List.of();
        }
        return locators.stream()
                .map(locator -> locatorQualityEvaluator.evaluate(pageUrl, element, locator))
                .filter(locatorQualityEvaluator::allowed)
                .sorted(Comparator.comparingDouble(LocatorCandidate::stabilityScore).reversed())
                .toList();
    }

    private boolean isClickable(PageElementModel element) {
        String text = (element.technicalType() + " " + element.tag() + " " + element.semanticType()).toLowerCase();
        return text.contains("button") || text.contains("link") || text.contains("submit") || text.contains("click");
    }

    private String semanticName(PageElementModel element) {
        return firstNonBlank(
                element.semanticType(),
                element.text(),
                element.name(),
                element.id(),
                element.elementId()
        );
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
