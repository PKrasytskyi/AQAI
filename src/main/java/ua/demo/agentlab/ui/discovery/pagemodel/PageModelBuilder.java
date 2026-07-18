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
import ua.demo.agentlab.ui.discovery.pagemodel.stage.ElementIdentityResolver;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.DiscoveredFormModelAssembler;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.FormModelAssembler;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.LocatorCandidateAssembler;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.PageActionAssembler;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.PageEvidenceAssembler;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.PageTransitionAssembler;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.RawElementNormalizer;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.SemanticElementAssembler;
import ua.demo.agentlab.ui.discovery.selenium.classification.ElementClassifier;
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
    private final RawElementNormalizer rawElementNormalizer = new RawElementNormalizer();
    private final ElementIdentityResolver identityResolver = new ElementIdentityResolver();
    private final LocatorCandidateAssembler locatorAssembler = new LocatorCandidateAssembler();
    private final PageActionAssembler actionAssembler = new PageActionAssembler(identityResolver);
    private final SemanticElementAssembler elementAssembler = new SemanticElementAssembler(
            elementClassifier, identityResolver, locatorAssembler, actionAssembler);
    private final FormModelAssembler formAssembler = new FormModelAssembler();
    private final DiscoveredFormModelAssembler discoveredFormAssembler = new DiscoveredFormModelAssembler(
            identityResolver, locatorAssembler, actionAssembler, formAssembler);
    private final PageTransitionAssembler transitionAssembler = new PageTransitionAssembler();
    private final PageEvidenceAssembler evidenceAssembler = new PageEvidenceAssembler();

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
                forms.add(discoveredFormAssembler.assemble(
                        page.pageId(), page.forms().get(formIndex), formIndex, elements, idsByRawKey));
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
                evidenceAssembler.assemble(page.evidence()),
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
        Map<String, PageElementModel> elementsByRawIdentity = new LinkedHashMap<>();
        for (RawElement rawElement : rawElementNormalizer.normalize(rawElements)) {
            String rawId = safe(rawElement.rawElementId());
            String rawIdentity = rawIdentity(rawElement, rawId);
            PageElementModel existing = rawIdentity.isBlank() ? null : elementsByRawIdentity.get(rawIdentity);
            if (existing != null) {
                elementAssembler.index(idsByRawKey, existing, rawElement);
                continue;
            }
            PageElementModel element = elementAssembler.assemble(pageId, rawElement, output);
            output.add(element);
            if (!rawIdentity.isBlank()) elementsByRawIdentity.put(rawIdentity, element);
            elementAssembler.index(idsByRawKey, element, rawElement);
        }
    }

    private String rawIdentity(RawElement rawElement, String rawId) {
        if (rawElement == null) return "";
        String semanticKey = identityResolver.rawElementKey(rawElement.tag(), rawElement.text(), rawElement.id(),
                rawElement.name(), rawElement.href());
        return rawId.isBlank() ? semanticKey : rawId + "|" + semanticKey;
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

    private PageFlowModel toFlowModel(DiscoveredTransition transition) {
        return transitionAssembler.assemble(transition);
    }

    private List<PageLocatorModel> buildLocatorCandidates(
            LocatorHint locatorHint,
            String id,
            String name,
            String href,
            String text
    ) {
        return locatorAssembler.fromHint(locatorHint, id, name, href, text);
    }

    private List<PageActionModel> inferActions(
            String elementId,
            String technicalType,
            String text,
            String href,
            boolean enabled
    ) {
        return actionAssembler.infer(elementId, technicalType, text, href, enabled);
    }

    private List<PageActionModel> inferFieldActions(String elementId, String fieldType) {
        return actionAssembler.forField(elementId, fieldType);
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

    private String normalizeRoute(String url) {
        String route = RouteCanonicalizer.canonicalize(url);
        return route.isBlank() ? "/" : route;
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
