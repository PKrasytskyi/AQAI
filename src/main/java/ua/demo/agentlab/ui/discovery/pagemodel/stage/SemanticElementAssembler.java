package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageActionModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.selenium.classification.ElementClassifier;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;

import java.util.List;
import java.util.Map;

/** Converts normalized raw facts into semantic element contracts. */
public final class SemanticElementAssembler {

    private final ElementClassifier classifier;
    private final ElementIdentityResolver identities;
    private final LocatorCandidateAssembler locators;
    private final PageActionAssembler actions;

    public SemanticElementAssembler(ElementClassifier classifier, ElementIdentityResolver identities,
                                    LocatorCandidateAssembler locators, PageActionAssembler actions) {
        this.classifier = classifier;
        this.identities = identities;
        this.locators = locators;
        this.actions = actions;
    }

    public PageElementModel assemble(String pageId, RawElement raw, List<PageElementModel> existing) {
        var classification = classifier.classify(pageId, raw);
        String suggested = classification.elementId().substring(classification.elementId().lastIndexOf(':') + 1);
        String elementId = identities.uniqueElementId(existing, pageId, suggested);
        List<PageLocatorModel> locatorCandidates = locators.fromRaw(raw);
        List<PageActionModel> actionCandidates = actions.fromClassification(elementId, classification.actions(),
                classification.confidenceScore());
        return new PageElementModel(elementId, classification.technicalType(), classification.semanticType(), raw.tag(),
                raw.type(), raw.text(), raw.id(), raw.name(), raw.placeholder(), raw.ariaLabel(), raw.role(), raw.href(),
                raw.cssClass(), raw.visible(), raw.enabled(), raw.required(), raw.attributes(), locatorCandidates,
                locatorCandidates.stream().findFirst().orElse(null), actionCandidates, classification.confidenceScore());
    }

    public void index(Map<String, String> index, PageElementModel element, RawElement raw) {
        index.put(identities.rawElementKey(element.technicalType(), raw.text(), raw.id(), raw.name(), raw.href()),
                element.elementId());
        index.put(identities.rawElementKey(raw.tag(), raw.text(), raw.id(), raw.name(), raw.href()), element.elementId());
        if (containsAny(element.technicalType(), "input", "dropdown", "select", "textarea")
                || containsAny(raw.tag(), "input", "select", "textarea")) {
            index.put(identities.rawElementKey("FIELD", raw.text(), raw.id(), raw.name(), ""), element.elementId());
        }
        if (containsAny(element.technicalType(), "button", "submit") || containsAny(raw.tag(), "button")
                || "submit".equalsIgnoreCase(raw.type())) {
            index.put(identities.rawElementKey("BUTTON", raw.text(), raw.id(), raw.name(), raw.href()), element.elementId());
        }
    }

    private boolean containsAny(String value, String... fragments) {
        String normalized = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        for (String fragment : fragments) if (normalized.contains(fragment)) return true;
        return false;
    }
}
