package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFormModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PageModelFormMapper {

    private final LocatorQualityEvaluator locatorQualityEvaluator;

    public PageModelFormMapper() {
        this(new LocatorQualityEvaluator());
    }

    public PageModelFormMapper(LocatorQualityEvaluator locatorQualityEvaluator) {
        this.locatorQualityEvaluator = locatorQualityEvaluator == null ? new LocatorQualityEvaluator() : locatorQualityEvaluator;
    }

    public MappedForm map(String pageUrl, PageFormModel form, Map<String, PageElementModel> elementsById) {
        List<MappedField> fields = form.fieldElementIds().stream()
                .map(elementsById::get)
                .filter(element -> element != null)
                .map(element -> mapField(pageUrl, element))
                .toList();
        return new MappedForm(
                form.formId(),
                form.formName(),
                form.action(),
                fields,
                form.submitElementIds().stream()
                        .map(elementsById::get)
                        .filter(element -> element != null)
                        .flatMap(element -> element.actions().stream())
                        .map(action -> action.actionId())
                        .distinct()
                        .toList()
        );
    }

    private MappedField mapField(String pageUrl, PageElementModel element) {
        return new MappedField(
                element.elementId() + ":field",
                firstNonBlank(element.name(), element.id(), element.text(), element.semanticType()),
                firstNonBlank(element.inputType(), element.technicalType()),
                element.text(),
                element.required(),
                element.placeholder(),
                mapLocators(pageUrl, element, element.locatorCandidates())
        );
    }

    private List<LocatorCandidate> mapLocators(String pageUrl, PageElementModel element, List<PageLocatorModel> locators) {
        return locators.stream()
                .map(locator -> locatorQualityEvaluator.evaluate(pageUrl, element, locator))
                .filter(locatorQualityEvaluator::allowed)
                .sorted(Comparator.comparingDouble(LocatorCandidate::stabilityScore).reversed())
                .toList();
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
