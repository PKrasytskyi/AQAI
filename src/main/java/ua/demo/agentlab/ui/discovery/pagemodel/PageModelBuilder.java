package ua.demo.agentlab.ui.discovery.pagemodel;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.pagemodel.enrichment.NoOpPageModelEnricher;
import ua.demo.agentlab.ui.discovery.pagemodel.enrichment.PageModelEnricher;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageActionModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageApiRelationModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageEvidenceModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFlowModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFormModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.selenium.classification.ElementClassification;
import ua.demo.agentlab.ui.discovery.selenium.classification.ElementClassifier;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredField;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveryLocatorKey;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PageModelBuilder {

    private final ElementClassifier elementClassifier = new ElementClassifier();
    private final PageFeatureClassifier pageFeatureClassifier = new PageFeatureClassifier();
    private final PageModelEnricher pageModelEnricher;

    public PageModelBuilder() {
        this(new NoOpPageModelEnricher());
    }

    public PageModelBuilder(PageModelEnricher pageModelEnricher) {
        this.pageModelEnricher = pageModelEnricher == null ? new NoOpPageModelEnricher() : pageModelEnricher;
    }

    public PageModelBundle build(UiDiscoverySnapshot snapshot, SeleniumDiscoveryResult seleniumDiscoveryResult) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }
        if (seleniumDiscoveryResult != null && seleniumDiscoveryResult.pages() != null
                && !seleniumDiscoveryResult.pages().isEmpty()) {
            return pageModelEnricher.enrich(buildFromSelenium(snapshot, seleniumDiscoveryResult));
        }
        return pageModelEnricher.enrich(buildFromSnapshot(snapshot));
    }

    private PageModelBundle buildFromSelenium(UiDiscoverySnapshot snapshot, SeleniumDiscoveryResult seleniumDiscoveryResult) {
        Map<String, List<PageFlowModel>> flowsByPageId = seleniumDiscoveryResult.transitions() == null
                ? Map.of()
                : seleniumDiscoveryResult.transitions().stream()
                .map(this::toFlowModel)
                .collect(Collectors.groupingBy(
                        PageFlowModel::fromPageId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PageModel> pages = new ArrayList<>(seleniumDiscoveryResult.pages().stream()
                .filter(page -> !isBrowserErrorPage(page))
                .map(page -> withLocatorStability(
                        toPageModel(page, flowsByPageId.getOrDefault(page.pageId(), List.of())),
                        seleniumDiscoveryResult
                ))
                .toList());
        Set<String> seleniumKeys = pages.stream()
                .flatMap(page -> java.util.stream.Stream.of(normalizeRoute(page.route()), sanitize(page.pageId())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        snapshot.pages().stream()
                .filter(page -> !seleniumKeys.contains(normalizeRoute(page.route()))
                        && !seleniumKeys.contains(sanitize(page.pageName())))
                .map(this::toSnapshotOnlyPageModel)
                .forEach(pages::add);
        pages = pages.stream()
                .sorted(Comparator.comparing(PageModel::pageId))
                .toList();
        return new PageModelBundle(pages);
    }

    private PageModel withLocatorStability(PageModel page, SeleniumDiscoveryResult seleniumDiscoveryResult) {
        if (page == null || seleniumDiscoveryResult == null || seleniumDiscoveryResult.discoveryRunCount() <= 1) {
            return page;
        }
        List<PageElementModel> elements = page.elements().stream()
                .map(element -> withLocatorStability(page.pageId(), element, seleniumDiscoveryResult))
                .toList();
        return new PageModel(
                page.pageId(),
                page.url(),
                page.route(),
                page.title(),
                page.visibleText(),
                page.featureGuess(),
                page.evidence(),
                elements,
                page.forms(),
                page.apiRelations(),
                page.flows()
        );
    }

    private PageElementModel withLocatorStability(
            String pageId,
            PageElementModel element,
            SeleniumDiscoveryResult seleniumDiscoveryResult
    ) {
        List<PageLocatorModel> locators = element.locatorCandidates().stream()
                .map(locator -> withLocatorStability(pageId, locator, seleniumDiscoveryResult))
                .sorted(Comparator.comparingDouble(PageLocatorModel::score).reversed())
                .toList();
        PageLocatorModel bestLocator = locators.stream().findFirst().orElse(null);
        return new PageElementModel(
                element.elementId(),
                element.technicalType(),
                element.semanticType(),
                element.tag(),
                element.inputType(),
                element.text(),
                element.id(),
                element.name(),
                element.placeholder(),
                element.ariaLabel(),
                element.role(),
                element.href(),
                element.cssClass(),
                element.visible(),
                element.enabled(),
                element.required(),
                element.attributes(),
                locators,
                bestLocator,
                element.actions(),
                element.confidenceScore()
        );
    }

    private PageLocatorModel withLocatorStability(
            String pageId,
            PageLocatorModel locator,
            SeleniumDiscoveryResult seleniumDiscoveryResult
    ) {
        int totalRuns = seleniumDiscoveryResult.discoveryRunCount();
        int observedRuns = seleniumDiscoveryResult.locatorObservationCounts()
                .getOrDefault(DiscoveryLocatorKey.key(pageId, locator.strategy(), locator.value()), 0);
        boolean stable = observedRuns >= totalRuns;
        return new PageLocatorModel(
                locator.strategy(),
                locator.value(),
                locator.score(),
                locator.reason(),
                locator.unique(),
                observedRuns,
                totalRuns,
                stable,
                locator.browserMatchCount(),
                locator.browserScopedMatchCount(),
                locator.browserScope()
        );
    }

    private PageModelBundle buildFromSnapshot(UiDiscoverySnapshot snapshot) {
        List<PageModel> pages = snapshot.pages().stream()
                .map(this::toSnapshotOnlyPageModel)
                .sorted(Comparator.comparing(PageModel::pageId))
                .toList();
        return new PageModelBundle(pages);
    }

    private PageModel toPageModel(DiscoveredPageSnapshot page, List<PageFlowModel> flows) {
        List<PageElementModel> elements = new ArrayList<>();
        Map<String, String> idsByRawKey = new LinkedHashMap<>();

        if (page.rawElements() != null && !page.rawElements().isEmpty()) {
            addRawElements(elements, idsByRawKey, page.pageId(), page.rawElements());
        } else {
            addInteractiveElements(elements, idsByRawKey, page.pageId(), page.links(), "a", "LINK");
            addInteractiveElements(elements, idsByRawKey, page.pageId(), page.buttons(), "button", "BUTTON");
        }

        List<PageFormModel> forms = new ArrayList<>();
        if (page.forms() != null) {
            for (int formIndex = 0; formIndex < page.forms().size(); formIndex++) {
                forms.add(toFormModel(page.pageId(), page.forms().get(formIndex), formIndex, elements, idsByRawKey));
            }
        }

        return new PageModel(
                page.pageId(),
                page.url(),
                normalizeRoute(page.url()),
                page.title(),
                page.rawPageSnapshot() == null || page.rawPageSnapshot().visibleText().isBlank()
                        ? buildVisibleText(page)
                        : page.rawPageSnapshot().visibleText(),
                pageFeatureClassifier.classify(page.capabilities(), page.url(), page.title()),
                page.evidence() == null
                        ? new PageEvidenceModel("", "")
                        : new PageEvidenceModel(page.evidence().screenshotPath(), page.evidence().htmlPath()),
                deduplicateElements(elements),
                forms,
                List.of(),
                flows
        );
    }

    private boolean isBrowserErrorPage(DiscoveredPageSnapshot page) {
        if (page == null) {
            return false;
        }
        String text = normalize(String.join(" ",
                safe(page.title()),
                page.rawPageSnapshot() == null ? "" : page.rawPageSnapshot().visibleText(),
                page.rawPageSnapshot() == null ? "" : page.rawPageSnapshot().currentUrl(),
                String.join(" ", page.headings()),
                String.join(" ", page.capabilities())));
        boolean browserErrorText = containsAny(text,
                "this page can't be found",
                "this site can't be reached",
                "page can t be found",
                "page cant be found",
                "chrome error",
                "404 not found",
                "not found",
                "reload");
        boolean browserErrorNetwork = page.rawPageSnapshot() != null
                && page.rawPageSnapshot().networkCalls().stream()
                .anyMatch(call -> call.status() >= 400
                        && "Document".equalsIgnoreCase(call.resourceType()));
        boolean reloadOnly = page.rawElements() != null
                && page.rawElements().size() == 1
                && normalize(page.rawElements().get(0).text()).equals("reload");
        return (browserErrorText || browserErrorNetwork)
                && (reloadOnly || containsAny(text, "can't be found", "can t be found", "cant be found", "not found"));
    }

    private void addRawElements(
            List<PageElementModel> output,
            Map<String, String> idsByRawKey,
            String pageId,
            List<RawElement> rawElements
    ) {
        for (RawElement rawElement : rawElements) {
            ElementClassification classification = elementClassifier.classify(pageId, rawElement);
            String elementId = uniqueElementId(output, pageId, classification.elementId().substring(classification.elementId().lastIndexOf(':') + 1));
            List<PageLocatorModel> locators = buildRawLocatorCandidates(rawElement);
            output.add(new PageElementModel(
                    elementId,
                    classification.technicalType(),
                    classification.semanticType(),
                    rawElement.tag(),
                    rawElement.type(),
                    rawElement.text(),
                    rawElement.id(),
                    rawElement.name(),
                    rawElement.placeholder(),
                    rawElement.ariaLabel(),
                    rawElement.role(),
                    rawElement.href(),
                    rawElement.cssClass(),
                    rawElement.visible(),
                    rawElement.enabled(),
                    rawElement.required(),
                    rawElement.attributes(),
                    locators,
                    locators.stream().findFirst().orElse(null),
                    classification.actions().stream()
                            .map(action -> new PageActionModel(
                                    elementId + ":action:" + sanitize(action),
                                    action,
                                    toActionName(action),
                                    "Inferred from browser DOM technical and semantic classification",
                                    classification.confidenceScore()
                            ))
                            .toList(),
                    classification.confidenceScore()
            ));
            idsByRawKey.put(rawElementKey(classification.technicalType(), rawElement.text(), rawElement.id(), rawElement.name(), rawElement.href()), elementId);
            idsByRawKey.put(rawElementKey(rawElement.tag(), rawElement.text(), rawElement.id(), rawElement.name(), rawElement.href()), elementId);
            if (containsAny(classification.technicalType(), "input", "dropdown", "select", "textarea")
                    || containsAny(rawElement.tag(), "input", "select", "textarea")) {
                idsByRawKey.put(rawElementKey("FIELD", rawElement.text(), rawElement.id(), rawElement.name(), ""), elementId);
            }
            if (containsAny(classification.technicalType(), "button", "submit")
                    || containsAny(rawElement.tag(), "button")
                    || "submit".equalsIgnoreCase(rawElement.type())) {
                idsByRawKey.put(rawElementKey("BUTTON", rawElement.text(), rawElement.id(), rawElement.name(), rawElement.href()), elementId);
            }
        }
    }

    private PageModel toSnapshotOnlyPageModel(DiscoveredUiPage page) {
        String pageId = sanitize(firstNonBlank(page.pageName(), page.route(), "page"));
        List<PageElementModel> elements = new ArrayList<>();
        for (int index = 0; index < page.locatorHints().size(); index++) {
            LocatorHint locatorHint = page.locatorHints().get(index);
            String semanticName = toSemanticName(firstNonBlank(locatorHint.elementName(), "element" + (index + 1)));
            String elementId = buildElementId(pageId, semanticName);
            List<PageLocatorModel> locators = buildLocatorCandidates(locatorHint, "", "", "", "");
            String technicalType = inferSnapshotTechnicalType(locatorHint);
            String evidenceText = safe(locatorHint.elementName()) + " " + safe(locatorHint.recommendedValue());
            String actionHref = "LINK".equals(technicalType) ? locatorHint.recommendedValue() : "";
            elements.add(new PageElementModel(
                    elementId,
                    technicalType,
                    inferSnapshotSemanticType(locatorHint, technicalType),
                    inferSnapshotTag(technicalType),
                    "",
                    locatorHint.elementName(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    true,
                    true,
                    false,
                    Map.of(),
                    locators,
                    locators.stream().findFirst().orElse(null),
                    inferActions(elementId, technicalType, evidenceText, actionHref, true),
                    0.50d
            ));
        }
        return new PageModel(
                pageId,
                page.route(),
                normalizeRoute(page.route()),
                page.pageName(),
                page.pageName(),
                pageFeatureClassifier.classify(page.capabilities(), page.route(), page.pageName()),
                new PageEvidenceModel("", ""),
                elements,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private void addInteractiveElements(
            List<PageElementModel> output,
            Map<String, String> idsByRawKey,
            String pageId,
            List<DiscoveredInteractiveElement> elements,
            String tag,
            String technicalType
    ) {
        if (elements == null) {
            return;
        }
        for (int index = 0; index < elements.size(); index++) {
            DiscoveredInteractiveElement element = elements.get(index);
            String semanticName = toSemanticName(firstNonBlank(
                    element.visibleText(),
                    element.name(),
                    element.id(),
                    tag + (index + 1)
            ));
            String elementId = uniqueElementId(output, pageId, semanticName);
            List<PageLocatorModel> locators = buildLocatorCandidates(
                    element.locatorHint(),
                    element.id(),
                    element.name(),
                    element.href(),
                    element.visibleText()
            );
            PageElementModel model = new PageElementModel(
                    elementId,
                    technicalType,
                    inferInteractiveSemanticType(element, technicalType),
                    tag,
                    "",
                    safe(element.visibleText()),
                    safe(element.id()),
                    safe(element.name()),
                    "",
                    "",
                    safe(element.role()),
                    safe(element.href()),
                    "",
                    element.visible(),
                    element.enabled(),
                    false,
                    attributes(
                            "href", element.href(),
                            "id", element.id(),
                            "name", element.name(),
                            "role", element.role()
                    ),
                    locators,
                    locators.stream().findFirst().orElse(null),
                    inferActions(elementId, technicalType, element.visibleText(), element.href(), element.enabled()),
                    0.82d
            );
            output.add(model);
            idsByRawKey.put(rawElementKey(technicalType, element.visibleText(), element.id(), element.name(), element.href()), elementId);
        }
    }

    private PageFormModel toFormModel(
            String pageId,
            DiscoveredForm form,
            int formIndex,
            List<PageElementModel> elements,
            Map<String, String> idsByRawKey
    ) {
        String formName = toSemanticName(firstNonBlank(form.formName(), form.formId(), "form" + (formIndex + 1)));
        String formId = pageId + ":form:" + sanitize(formName);
        List<String> fieldIds = new ArrayList<>();
        for (int fieldIndex = 0; fieldIndex < form.fields().size(); fieldIndex++) {
            DiscoveredField field = form.fields().get(fieldIndex);
            String rawKey = rawElementKey("FIELD", field.label(), field.id(), field.name(), "");
            String existingId = firstNonBlank(idsByRawKey.get(rawKey), findExistingFieldElementId(elements, field));
            if (!existingId.isBlank()) {
                fieldIds.add(existingId);
                idsByRawKey.put(rawKey, existingId);
                continue;
            }
            String fieldName = toSemanticName(firstNonBlank(field.label(), field.name(), field.id(), "field" + (fieldIndex + 1)));
            String elementId = uniqueElementId(elements, pageId, fieldName);
            List<PageLocatorModel> locators = buildLocatorCandidates(field.locatorHint(), field.id(), field.name(), "", field.label());
            PageElementModel fieldElement = new PageElementModel(
                    elementId,
                    inferFieldTechnicalType(field.fieldType()),
                    inferFieldSemanticType(field),
                    inferFieldTag(field.fieldType()),
                    safe(field.fieldType()),
                    safe(field.label()),
                    safe(field.id()),
                    safe(field.name()),
                    safe(field.placeholder()),
                    "",
                    "",
                    "",
                    "",
                    true,
                    true,
                    field.required(),
                    attributes(
                            "id", field.id(),
                            "name", field.name(),
                            "placeholder", field.placeholder(),
                            "type", field.fieldType()
                    ),
                    locators,
                    locators.stream().findFirst().orElse(null),
                    inferFieldActions(elementId, field.fieldType()),
                    0.82d
            );
            elements.add(fieldElement);
            fieldIds.add(elementId);
            idsByRawKey.put(rawElementKey("FIELD", field.label(), field.id(), field.name(), ""), elementId);
        }

        List<String> submitIds = new ArrayList<>();
        for (int submitIndex = 0; submitIndex < form.submitActions().size(); submitIndex++) {
            DiscoveredInteractiveElement submit = form.submitActions().get(submitIndex);
            String rawKey = rawElementKey("BUTTON", submit.visibleText(), submit.id(), submit.name(), submit.href());
            String existingId = firstNonBlank(idsByRawKey.get(rawKey), findExistingSubmitElementId(elements, submit));
            if (!existingId.isBlank()) {
                submitIds.add(existingId);
                idsByRawKey.put(rawKey, existingId);
                continue;
            }
            String semanticName = toSemanticName(firstNonBlank(
                    submit.visibleText(),
                    submit.name(),
                    submit.id(),
                    formName + "Submit"
            ));
            String elementId = uniqueElementId(elements, pageId, semanticName);
            List<PageLocatorModel> locators = new ArrayList<>(buildLocatorCandidates(
                    submit.locatorHint(),
                    submit.id(),
                    submit.name(),
                    submit.href(),
                    submit.visibleText()
            ));
            addLocator(locators, "css", "button[type='submit']", 0.82d, "submit control candidate");
            locators = deduplicateLocators(locators);
            PageElementModel submitElement = new PageElementModel(
                    elementId,
                    "SUBMIT_BUTTON",
                    inferInteractiveSemanticType(submit, "BUTTON"),
                    "button",
                    "submit",
                    safe(submit.visibleText()),
                    safe(submit.id()),
                    safe(submit.name()),
                    "",
                    "",
                    safe(submit.role()),
                    safe(submit.href()),
                    "",
                    submit.visible(),
                    submit.enabled(),
                    false,
                    attributes(
                            "id", submit.id(),
                            "name", submit.name(),
                            "role", submit.role(),
                            "type", "submit"
                    ),
                    locators,
                    locators.stream().findFirst().orElse(null),
                    inferActions(elementId, "SUBMIT_BUTTON", submit.visibleText(), submit.href(), submit.enabled()),
                    0.82d
            );
            elements.add(submitElement);
            submitIds.add(elementId);
            idsByRawKey.put(rawKey, elementId);
        }

        return new PageFormModel(formId, formName, form.action(), fieldIds, submitIds);
    }

    private String findExistingFieldElementId(List<PageElementModel> elements, DiscoveredField field) {
        String fieldId = normalize(field.id());
        String fieldName = normalize(field.name());
        String fieldLabel = normalize(field.label());
        String fieldPlaceholder = normalize(field.placeholder());
        return elements.stream()
                .filter(element -> containsAny(element.technicalType(), "input", "dropdown", "select", "textarea")
                        || containsAny(element.tag(), "input", "select", "textarea"))
                .filter(element -> matchesAny(
                        normalize(element.id()),
                        fieldId,
                        fieldName,
                        fieldLabel,
                        fieldPlaceholder
                ) || matchesAny(
                        normalize(element.name()),
                        fieldId,
                        fieldName,
                        fieldLabel,
                        fieldPlaceholder
                ) || matchesAny(
                        normalize(element.text()),
                        fieldId,
                        fieldName,
                        fieldLabel,
                        fieldPlaceholder
                ) || matchesAny(
                        normalize(element.placeholder()),
                        fieldId,
                        fieldName,
                        fieldLabel,
                        fieldPlaceholder
                ))
                .map(PageElementModel::elementId)
                .findFirst()
                .orElse("");
    }

    private String findExistingSubmitElementId(List<PageElementModel> elements, DiscoveredInteractiveElement submit) {
        String submitId = normalize(submit.id());
        String submitName = normalize(submit.name());
        String submitText = normalize(submit.visibleText());
        return elements.stream()
                .filter(element -> containsAny(element.technicalType(), "button", "submit")
                        || containsAny(element.tag(), "button")
                        || "submit".equalsIgnoreCase(element.inputType()))
                .filter(element -> matchesAny(
                        normalize(element.id()),
                        submitId,
                        submitName,
                        submitText
                ) || matchesAny(
                        normalize(element.name()),
                        submitId,
                        submitName,
                        submitText
                ) || matchesAny(
                        normalize(element.text()),
                        submitId,
                        submitName,
                        submitText
                ))
                .map(PageElementModel::elementId)
                .findFirst()
                .orElse("");
    }

    private boolean matchesAny(String actual, String... expectedValues) {
        if (actual == null || actual.isBlank()) {
            return false;
        }
        for (String expected : expectedValues) {
            if (expected != null && !expected.isBlank() && actual.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private PageFlowModel toFlowModel(DiscoveredTransition transition) {
        String actionLabel = firstNonBlank(transition.actionLabel(), transition.actionType(), "action");
        return new PageFlowModel(
                transition.fromPageId() + ":flow:" + sanitize(actionLabel),
                transition.fromPageId(),
                actionLabel,
                transition.actionType(),
                transition.toPageId(),
                transition.toUrl(),
                transition.success()
        );
    }

    private List<PageLocatorModel> buildLocatorCandidates(
            LocatorHint locatorHint,
            String id,
            String name,
            String href,
            String text
    ) {
        List<PageLocatorModel> locators = new ArrayList<>();
        if (locatorHint != null && locatorHint.recommendedValue() != null && !locatorHint.recommendedValue().isBlank()) {
            locators.add(new PageLocatorModel(
                    locatorHint.recommendedStrategy(),
                    locatorHint.recommendedValue(),
                    locatorScore(locatorHint.recommendedStrategy(), "discovered locator hint"),
                    "discovered locator hint",
                    false
            ));
        }
        addLocator(locators, "id", id, 0.90d, "stable id candidate");
        addLocator(locators, "name", name, 0.84d, "name attribute candidate");
        if (href != null && !href.isBlank() && !isAbsoluteHttpUrl(href)) {
            addLocator(locators, "css", "a[href='" + escapeCssValue(href) + "']", 0.76d, "href candidate");
        }
        if (text != null && !text.isBlank()) {
            addLocator(locators, "xpath", "//*[normalize-space()='" + escapeXpathLiteral(text) + "']", 0.58d, "visible text fallback");
        }
        return locators.stream()
                .filter(locator -> !locator.value().isBlank())
                .filter(locator -> !containsAbsoluteHttpUrl(locator.value()))
                .collect(Collectors.toMap(
                        locator -> locator.strategy() + "::" + locator.value(),
                        locator -> locator,
                        (left, right) -> left.score() >= right.score() ? left : right,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(PageLocatorModel::score).reversed())
                .toList();
    }

    private List<PageLocatorModel> buildRawLocatorCandidates(RawElement rawElement) {
        List<PageLocatorModel> locators = new ArrayList<>();
        addRuntimeLocator(locators, rawElement, "css", dataAttributeLocator(rawElement), 0.95d, "stable data-test attribute");
        addRuntimeLocator(locators, rawElement, "css", rawElement.ariaLabel().isBlank()
                ? ""
                : rawElement.tag() + "[aria-label='" + escapeCssValue(rawElement.ariaLabel()) + "']", 0.88d, "aria-label attribute");
        addRuntimeLocator(locators, rawElement, "id", rawElement.id(), 0.90d, "stable id candidate");
        addRuntimeLocator(locators, rawElement, "name", rawElement.name(), 0.84d, "name attribute candidate");
        addRuntimeLocator(locators, rawElement, "css", rawElement.href().isBlank() || isAbsoluteHttpUrl(rawElement.href())
                ? ""
                : rawElement.tag() + "[href='" + escapeCssValue(rawElement.href()) + "']", 0.78d, "href attribute");
        addRuntimeLocator(locators, rawElement, "css", rawElement.placeholder().isBlank()
                ? ""
                : rawElement.tag() + "[placeholder='" + escapeCssValue(rawElement.placeholder()) + "']", 0.72d, "placeholder attribute");
        addRuntimeLocator(locators, rawElement, "css", stableClassLocator(rawElement), 0.78d, "stable semantic class");
        addRuntimeLocator(locators, rawElement, "css", submitControlLocator(rawElement), 0.82d, "submit control candidate");
        if (!rawElement.text().isBlank()
                && ("button".equals(rawElement.tag()) || "a".equals(rawElement.tag()) || headingTag(rawElement.tag()))) {
            addRuntimeLocator(locators, rawElement, "xpath", "//" + rawElement.tag() + "[normalize-space()='" + escapeXpathLiteral(rawElement.text()) + "']", 0.62d, "button/link text fallback");
        }
        return locators.stream()
                .filter(locator -> !locator.value().isBlank())
                .filter(locator -> !containsAbsoluteHttpUrl(locator.value()))
                .collect(Collectors.toMap(
                        locator -> locator.strategy() + "::" + locator.value(),
                        locator -> locator,
                        (left, right) -> left.score() >= right.score() ? left : right,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(PageLocatorModel::score).reversed())
                .toList();
    }

    private String dataAttributeLocator(RawElement rawElement) {
        if (rawElement.dataTestId().isBlank()) {
            return "";
        }
        if (rawElement.attributes().containsKey("data-testid")) {
            return "[data-testid='" + escapeCssValue(rawElement.dataTestId()) + "']";
        }
        if (rawElement.attributes().containsKey("data-test")) {
            return "[data-test='" + escapeCssValue(rawElement.dataTestId()) + "']";
        }
        if (rawElement.attributes().containsKey("data-qa")) {
            return "[data-qa='" + escapeCssValue(rawElement.dataTestId()) + "']";
        }
        return "";
    }

    private List<PageLocatorModel> deduplicateLocators(List<PageLocatorModel> locators) {
        return locators.stream()
                .filter(locator -> !locator.value().isBlank())
                .filter(locator -> !containsAbsoluteHttpUrl(locator.value()))
                .collect(Collectors.toMap(
                        locator -> locator.strategy() + "::" + locator.value(),
                        locator -> locator,
                        (left, right) -> left.score() >= right.score() ? left : right,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(PageLocatorModel::score).reversed())
                .toList();
    }

    private String submitControlLocator(RawElement rawElement) {
        String tag = safe(rawElement.tag()).toLowerCase(Locale.ROOT);
        String type = safe(rawElement.type()).toLowerCase(Locale.ROOT);
        if (!"submit".equals(type)) {
            return "";
        }
        if ("button".equals(tag) || "input".equals(tag)) {
            return tag + "[type='submit']";
        }
        return "";
    }

    private String stableClassLocator(RawElement rawElement) {
        String tag = safe(rawElement.tag()).toLowerCase(Locale.ROOT);
        if (tag.isBlank() || rawElement.cssClass().isBlank()) {
            return "";
        }
        for (String token : rawElement.cssClass().split("\\s+")) {
            String normalized = token.toLowerCase(Locale.ROOT);
            if (stableSemanticClass(normalized)) {
                return tag + "." + escapeCssClass(token);
            }
        }
        return "";
    }

    private boolean stableSemanticClass(String token) {
        return token.contains("dropdown")
                || token.contains("breadcrumb")
                || token.contains("topbar")
                || token.contains("dashboard")
                || token.contains("header")
                || token.contains("title")
                || token.contains("menu")
                || token.contains("logout")
                || token.contains("button")
                || token.contains("link");
    }

    private boolean headingTag(String tag) {
        return safe(tag).toLowerCase(Locale.ROOT).matches("h[1-6]");
    }

    private boolean isAbsoluteHttpUrl(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private boolean containsAbsoluteHttpUrl(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("http://") || normalized.contains("https://");
    }

    private List<PageActionModel> inferActions(
            String elementId,
            String technicalType,
            String text,
            String href,
            boolean enabled
    ) {
        if (!enabled) {
            return List.of();
        }
        Set<String> actionTypes = new LinkedHashSet<>();
        String normalized = normalize(text + " " + href + " " + technicalType);
        if (technicalType.contains("LINK") || href != null && !href.isBlank()) {
            if (containsAny(normalized, "detail", "details", "view", "record")) {
                actionTypes.add("open-details");
            } else if (containsAny(normalized, "cart", "basket", "bag", "container")) {
                actionTypes.add("open-target-container");
            } else {
                actionTypes.add("click");
            }
        } else if (technicalType.contains("BUTTON")) {
            if (containsAny(normalized, "remove", "delete", "clear")) {
                actionTypes.add("remove-entity-from-container");
            } else if (containsAny(normalized, "add", "cart/add", "buy")) {
                actionTypes.add("add-entity-to-container");
            } else if (containsAny(normalized, "search")) {
                actionTypes.add("search");
            } else {
                actionTypes.add("click");
            }
        } else if (technicalType.contains("INPUT")) {
            actionTypes.add("type");
            actionTypes.add("clear");
        } else if (!technicalType.contains("UNKNOWN")) {
            actionTypes.add("click");
        }
        return actionTypes.stream()
                .map(actionType -> new PageActionModel(
                        elementId + ":action:" + sanitize(actionType),
                        actionType,
                        toActionName(actionType),
                        "Inferred from technical type and visible element evidence",
                        0.72d
                ))
                .toList();
    }

    private List<PageActionModel> inferFieldActions(String elementId, String fieldType) {
        String normalized = normalize(fieldType);
        if ("select".equals(normalized)) {
            return List.of(new PageActionModel(
                    elementId + ":action:select",
                    "select",
                    "selectValue",
                    "Inferred from select field",
                    0.86d
            ));
        }
        if ("checkbox".equals(normalized)) {
            return List.of(
                    new PageActionModel(elementId + ":action:check", "check", "check", "Inferred from checkbox field", 0.86d),
                    new PageActionModel(elementId + ":action:uncheck", "uncheck", "uncheck", "Inferred from checkbox field", 0.86d)
            );
        }
        if ("radio".equals(normalized)) {
            return List.of(new PageActionModel(
                    elementId + ":action:select",
                    "select",
                    "selectRadio",
                    "Inferred from radio field",
                    0.86d
            ));
        }
        if ("file".equals(normalized)) {
            return List.of(new PageActionModel(
                    elementId + ":action:upload",
                    "upload",
                    "uploadFile",
                    "Inferred from file input",
                    0.86d
            ));
        }
        return List.of(
                new PageActionModel(elementId + ":action:type", "type", "typeText", "Inferred from input field", 0.86d),
                new PageActionModel(elementId + ":action:clear", "clear", "clear", "Inferred from input field", 0.80d)
        );
    }

    private List<PageElementModel> deduplicateElements(List<PageElementModel> elements) {
        return elements.stream()
                .collect(Collectors.toMap(
                        PageElementModel::elementId,
                        element -> element,
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    private String uniqueElementId(List<PageElementModel> elements, String pageId, String semanticName) {
        String baseId = buildElementId(pageId, semanticName);
        Set<String> existingIds = elements.stream()
                .map(PageElementModel::elementId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!existingIds.contains(baseId)) {
            return baseId;
        }
        int index = 2;
        while (existingIds.contains(baseId + "-" + index)) {
            index++;
        }
        return baseId + "-" + index;
    }

    private String buildElementId(String pageId, String semanticName) {
        return safe(pageId) + ":element:" + sanitize(semanticName);
    }

    private void addLocator(List<PageLocatorModel> locators, String strategy, String value, double score, String reason) {
        if (value == null || value.isBlank()) {
            return;
        }
        locators.add(new PageLocatorModel(strategy, value, score, reason, false));
    }

    private void addRuntimeLocator(
            List<PageLocatorModel> locators,
            RawElement rawElement,
            String strategy,
            String value,
            double score,
            String reason
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        String key = locatorRuntimeKey(strategy, value);
        int browserMatchCount = rawElement.locatorMatchCounts().getOrDefault(key, -1);
        int browserScopedMatchCount = rawElement.locatorScopedMatchCounts().getOrDefault(key, -1);
        boolean browserUnique = browserMatchCount == 1 || browserScopedMatchCount == 1;
        locators.add(new PageLocatorModel(
                strategy,
                value,
                score,
                reason,
                browserUnique,
                1,
                1,
                true,
                browserMatchCount,
                browserScopedMatchCount,
                rawElement.locatorScopes().getOrDefault(key, "")
        ));
    }

    private String locatorRuntimeKey(String strategy, String value) {
        return safe(strategy).toLowerCase(Locale.ROOT) + "::" + safe(value);
    }

    private String inferInteractiveSemanticType(DiscoveredInteractiveElement element, String technicalType) {
        String text = normalize(element.visibleText() + " " + element.name() + " " + element.id() + " " + element.href());
        if (technicalType.contains("LINK")) {
            if (containsAny(text, "cart", "basket", "bag")) {
                return "CART_LINK";
            }
            if (containsAny(text, "detail", "details", "view", "record")) {
                return "DETAILS_LINK";
            }
            return "NAVIGATION_LINK";
        }
        if (containsAny(text, "add", "cart", "buy")) {
            return "ADD_TO_CART_BUTTON";
        }
        if (containsAny(text, "remove", "delete", "clear")) {
            return "REMOVE_BUTTON";
        }
        if (containsAny(text, "search")) {
            return "SEARCH_BUTTON";
        }
        return "BUTTON";
    }

    private String inferSnapshotTechnicalType(LocatorHint locatorHint) {
        String text = normalize(locatorHint.elementName() + " " + locatorHint.recommendedStrategy() + " " + locatorHint.recommendedValue());
        if (containsAny(text, "button", "submit", "add", "remove", "checkout")) {
            return "BUTTON";
        }
        if (containsAny(text, "a[href", "link", "href")) {
            return "LINK";
        }
        if (containsAny(text, "input", "search")) {
            return "INPUT";
        }
        return "UNKNOWN";
    }

    private String inferSnapshotSemanticType(LocatorHint locatorHint, String technicalType) {
        String text = normalize(locatorHint.elementName() + " " + locatorHint.recommendedValue());
        if (containsAny(text, "title")) {
            return "PAGE_TITLE";
        }
        if (containsAny(text, "listing container")) {
            return "COLLECTION_CONTAINER";
        }
        if (containsAny(text, "item card")) {
            return "ENTITY_CARD";
        }
        if (containsAny(text, "container item")) {
            return "CONTAINER_ITEM";
        }
        if (containsAny(text, "empty state")) {
            return "EMPTY_STATE";
        }
        if (containsAny(text, "remove", "delete", "clear")) {
            return "REMOVE_BUTTON";
        }
        if (containsAny(text, "add", "cart/add", "buy")) {
            return "ADD_TO_CART_BUTTON";
        }
        if (containsAny(text, "details", "detail", "view", "record link", "item link")) {
            return "DETAILS_LINK";
        }
        if (containsAny(text, "cart", "basket", "bag", "destination container")) {
            return "CART_LINK";
        }
        if (containsAny(text, "search")) {
            return "SEARCH_FIELD";
        }
        return technicalType;
    }

    private String inferSnapshotTag(String technicalType) {
        return switch (technicalType) {
            case "LINK" -> "a";
            case "BUTTON" -> "button";
            case "INPUT" -> "input";
            default -> "";
        };
    }

    private String inferFieldTechnicalType(String fieldType) {
        String normalized = normalize(fieldType);
        return switch (normalized) {
            case "email" -> "EMAIL_INPUT";
            case "password" -> "PASSWORD_INPUT";
            case "select" -> "DROPDOWN";
            case "textarea" -> "TEXTAREA";
            case "checkbox" -> "CHECKBOX";
            case "radio" -> "RADIO";
            case "file" -> "FILE_INPUT";
            default -> "INPUT";
        };
    }

    private String inferFieldSemanticType(DiscoveredField field) {
        String text = normalize(field.fieldType() + " " + field.label() + " " + field.name() + " " + field.id());
        if (containsAny(text, "email")) {
            return "EMAIL";
        }
        if (containsAny(text, "password")) {
            return "PASSWORD";
        }
        if (containsAny(text, "size")) {
            return "SIZE";
        }
        if (containsAny(text, "color", "colour")) {
            return "COLOR";
        }
        if (containsAny(text, "quantity", "qty")) {
            return "QUANTITY";
        }
        return "FIELD";
    }

    private String inferFieldTag(String fieldType) {
        String normalized = normalize(fieldType);
        if ("select".equals(normalized)) {
            return "select";
        }
        if ("textarea".equals(normalized)) {
            return "textarea";
        }
        return "input";
    }

    private String buildVisibleText(DiscoveredPageSnapshot page) {
        List<String> parts = new ArrayList<>();
        parts.add(page.title());
        parts.addAll(page.headings());
        addElementText(parts, page.links());
        addElementText(parts, page.buttons());
        if (page.forms() != null) {
            for (DiscoveredForm form : page.forms()) {
                form.fields().forEach(field -> parts.add(firstNonBlank(field.label(), field.placeholder(), field.name(), field.id())));
                addElementText(parts, form.submitActions());
            }
        }
        return parts.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(80)
                .collect(Collectors.joining(" "));
    }

    private void addElementText(List<String> parts, List<DiscoveredInteractiveElement> elements) {
        if (elements == null) {
            return;
        }
        elements.forEach(element -> parts.add(firstNonBlank(element.visibleText(), element.name(), element.id())));
    }

    private Map<String, String> attributes(String... pairs) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            String key = pairs[index];
            String value = pairs[index + 1];
            if (key != null && value != null && !value.isBlank()) {
                attributes.put(key, value);
            }
        }
        return attributes;
    }

    private String rawElementKey(String type, String text, String id, String name, String href) {
        return normalize(type + "|" + text + "|" + id + "|" + name + "|" + href);
    }

    private String toActionName(String actionType) {
        return switch (actionType) {
            case "open-details" -> "openDetails";
            case "open-target-container" -> "openTargetContainer";
            case "add-entity-to-container" -> "addEntityToContainer";
            case "remove-entity-from-container" -> "removeEntityFromContainer";
            case "select-by-visible-text" -> "selectByVisibleText";
            case "select-by-value" -> "selectByValue";
            default -> toSemanticName(actionType);
        };
    }

    private String toSemanticName(String value) {
        String[] tokens = safe(value).split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (builder.isEmpty()) {
                builder.append(lower);
            } else {
                builder.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
        }
        return builder.isEmpty() ? "element" : builder.toString();
    }

    private double locatorScore(String strategy, String reason) {
        String normalizedStrategy = normalize(strategy);
        String normalizedReason = normalize(reason);
        if (containsAny(normalizedStrategy, "data-testid", "data-test", "data-qa")
                || containsAny(normalizedReason, "test")) {
            return 0.95d;
        }
        return switch (normalizedStrategy) {
            case "id" -> 0.90d;
            case "name" -> 0.84d;
            case "css" -> 0.76d;
            case "partiallinktext", "linktext" -> 0.68d;
            case "xpath" -> 0.58d;
            default -> 0.62d;
        };
    }

    private String normalizeRoute(String url) {
        String route = RouteCanonicalizer.canonicalize(url);
        return route.isBlank() ? "/" : route;
    }

    private String escapeCssValue(String value) {
        return safe(value).replace("\\", "\\\\").replace("'", "\\'");
    }

    private String escapeCssClass(String value) {
        return safe(value).replace("\\", "\\\\").replace(".", "\\.");
    }

    private String escapeXpathLiteral(String value) {
        return safe(value).replace("'", "\\'");
    }

    private String sanitize(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "value" : normalized;
    }

    private String normalize(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean containsAny(String text, String... fragments) {
        String normalized = normalize(text);
        for (String fragment : fragments) {
            if (normalized.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }
}
