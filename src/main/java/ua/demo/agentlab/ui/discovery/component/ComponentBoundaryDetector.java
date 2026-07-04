package ua.demo.agentlab.ui.discovery.component;

import ua.demo.agentlab.ui.discovery.component.model.ComponentDiscoveryModel;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentModel;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentPageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFormModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ComponentBoundaryDetector {

    private final ScopedLocatorValidationService scopedLocatorValidationService;

    public ComponentBoundaryDetector() {
        this(new ScopedLocatorValidationService());
    }

    public ComponentBoundaryDetector(ScopedLocatorValidationService scopedLocatorValidationService) {
        this.scopedLocatorValidationService = scopedLocatorValidationService == null
                ? new ScopedLocatorValidationService()
                : scopedLocatorValidationService;
    }

    public ComponentDiscoveryModel detect(PageModelBundle bundle) {
        if (bundle == null || bundle.pages().isEmpty()) {
            return ComponentDiscoveryModel.empty("component:no-page-models");
        }
        List<SemanticComponentPageModel> pages = bundle.pages().stream()
                .map(this::detectPage)
                .toList();
        return new ComponentDiscoveryModel(pages, List.of("component:page-model-bundle"));
    }

    private SemanticComponentPageModel detectPage(PageModel page) {
        Map<String, PageElementModel> elementsById = page.elements().stream()
                .collect(Collectors.toMap(
                        PageElementModel::elementId,
                        element -> element,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, Set<String>> locatorIndex = locatorIndex(page.elements());
        List<SemanticComponentModel> components = new ArrayList<>();
        Set<String> assigned = new LinkedHashSet<>();

        addFormComponents(page, elementsById, locatorIndex, components, assigned);
        addSearchComponent(page, elementsById, locatorIndex, components, assigned);
        addNavigationComponent(page, elementsById, locatorIndex, components, assigned);
        addTableComponent(page, elementsById, locatorIndex, components, assigned);
        addContentComponent(page, elementsById, locatorIndex, components, assigned);

        double confidence = components.stream()
                .mapToDouble(SemanticComponentModel::confidence)
                .average()
                .orElse(0.0d);
        return new SemanticComponentPageModel(
                page.pageId(),
                page.route(),
                page.featureGuess(),
                components,
                confidence
        );
    }

    private void addFormComponents(
            PageModel page,
            Map<String, PageElementModel> elementsById,
            Map<String, Set<String>> locatorIndex,
            List<SemanticComponentModel> components,
            Set<String> assigned
    ) {
        for (PageFormModel form : page.forms()) {
            List<String> elementIds = new ArrayList<>();
            elementIds.addAll(form.fieldElementIds());
            elementIds.addAll(form.submitElementIds());
            elementIds = elementIds.stream()
                    .filter(elementsById::containsKey)
                    .distinct()
                    .toList();
            if (elementIds.isEmpty()) {
                continue;
            }
            String componentId = page.pageId() + ":component:" + sanitize(firstNonBlank(form.formName(), "form"));
            components.add(component(
                    page,
                    componentId,
                    toDisplayName(firstNonBlank(form.formName(), "form")),
                    ComponentType.FORM,
                    elementIds,
                    elementsById,
                    locatorIndex,
                    0.90d,
                    List.of(),
                    List.of("component:form", "formId=" + form.formId())
            ));
            assigned.addAll(elementIds);
        }
    }

    private void addSearchComponent(
            PageModel page,
            Map<String, PageElementModel> elementsById,
            Map<String, Set<String>> locatorIndex,
            List<SemanticComponentModel> components,
            Set<String> assigned
    ) {
        List<String> elementIds = elementsById.values().stream()
                .filter(element -> !assigned.contains(element.elementId()))
                .filter(this::isSearchElement)
                .map(PageElementModel::elementId)
                .toList();
        if (elementIds.isEmpty()) {
            return;
        }
        components.add(component(
                page,
                page.pageId() + ":component:search",
                "SearchComponent",
                ComponentType.SEARCH,
                elementIds,
                elementsById,
                locatorIndex,
                0.82d,
                List.of(),
                List.of("component:search-evidence")
        ));
        assigned.addAll(elementIds);
    }

    private void addNavigationComponent(
            PageModel page,
            Map<String, PageElementModel> elementsById,
            Map<String, Set<String>> locatorIndex,
            List<SemanticComponentModel> components,
            Set<String> assigned
    ) {
        List<String> elementIds = elementsById.values().stream()
                .filter(element -> !assigned.contains(element.elementId()))
                .filter(this::isNavigationElement)
                .map(PageElementModel::elementId)
                .toList();
        if (elementIds.size() < 2) {
            return;
        }
        components.add(component(
                page,
                page.pageId() + ":component:navigation",
                "NavigationComponent",
                ComponentType.NAVIGATION,
                elementIds,
                elementsById,
                locatorIndex,
                0.78d,
                List.of("navigation-actions-are-not-page-owned-without-requirement-scope"),
                List.of("component:navigation-link-group")
        ));
        assigned.addAll(elementIds);
    }

    private void addTableComponent(
            PageModel page,
            Map<String, PageElementModel> elementsById,
            Map<String, Set<String>> locatorIndex,
            List<SemanticComponentModel> components,
            Set<String> assigned
    ) {
        List<String> elementIds = elementsById.values().stream()
                .filter(element -> !assigned.contains(element.elementId()))
                .filter(this::isTableElement)
                .map(PageElementModel::elementId)
                .toList();
        if (elementIds.isEmpty()) {
            return;
        }
        components.add(component(
                page,
                page.pageId() + ":component:table",
                "TableComponent",
                ComponentType.TABLE,
                elementIds,
                elementsById,
                locatorIndex,
                0.76d,
                List.of(),
                List.of("component:table-evidence")
        ));
        assigned.addAll(elementIds);
    }

    private void addContentComponent(
            PageModel page,
            Map<String, PageElementModel> elementsById,
            Map<String, Set<String>> locatorIndex,
            List<SemanticComponentModel> components,
            Set<String> assigned
    ) {
        List<String> elementIds = elementsById.values().stream()
                .filter(element -> !assigned.contains(element.elementId()))
                .map(PageElementModel::elementId)
                .toList();
        if (elementIds.isEmpty()) {
            return;
        }
        ComponentType type = page.forms().isEmpty() && components.isEmpty() ? ComponentType.CONTENT : ComponentType.CONTENT;
        components.add(component(
                page,
                page.pageId() + ":component:content",
                "ContentComponent",
                type,
                elementIds,
                elementsById,
                locatorIndex,
                0.62d,
                List.of("fallback-component-boundary"),
                List.of("component:unassigned-visible-elements")
        ));
    }

    private SemanticComponentModel component(
            PageModel page,
            String componentId,
            String name,
            ComponentType type,
            List<String> elementIds,
            Map<String, PageElementModel> elementsById,
            Map<String, Set<String>> locatorIndex,
            double baseConfidence,
            List<String> risks,
            List<String> sourceTrace
    ) {
        List<PageElementModel> elements = elementIds.stream()
                .map(elementsById::get)
                .filter(element -> element != null)
                .toList();
        List<ScopedLocatorCandidate> locators = scopedLocatorValidationService.validate(
                page,
                componentId,
                elements,
                locatorIndex
        );
        double locatorConfidence = locators.stream()
                .mapToDouble(ScopedLocatorCandidate::finalScore)
                .average()
                .orElse(0.0d);
        double confidence = Math.max(baseConfidence, locatorConfidence);
        PageLocatorModel root = inferRootLocator(type, elements);
        return new SemanticComponentModel(
                page.pageId(),
                componentId,
                name,
                type,
                root == null ? "" : root.strategy(),
                root == null ? "" : root.value(),
                elementIds,
                locators,
                reusable(type, elementIds),
                confidence,
                risks,
                sourceTrace
        );
    }

    private PageLocatorModel inferRootLocator(ComponentType type, List<PageElementModel> elements) {
        if (type == ComponentType.FORM) {
            return new PageLocatorModel("css", "form", 0.50d, "component root fallback", false);
        }
        if (type == ComponentType.NAVIGATION) {
            return new PageLocatorModel("css", "nav, aside, [role='navigation']", 0.50d, "component root fallback", false);
        }
        if (type == ComponentType.SEARCH) {
            return new PageLocatorModel("css", "input[type='search'], input[placeholder*='Search']", 0.50d, "component root fallback", false);
        }
        return elements.stream()
                .flatMap(element -> element.locatorCandidates().stream())
                .max(Comparator.comparingDouble(PageLocatorModel::score))
                .orElse(null);
    }

    private Map<String, Set<String>> locatorIndex(List<PageElementModel> elements) {
        Map<String, Set<String>> index = new LinkedHashMap<>();
        for (PageElementModel element : elements) {
            for (PageLocatorModel locator : element.locatorCandidates()) {
                String key = key(locator);
                if (key.isBlank()) {
                    continue;
                }
                index.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(element.elementId());
            }
        }
        return index;
    }

    private boolean reusable(ComponentType type, List<String> elementIds) {
        return type == ComponentType.NAVIGATION
                || type == ComponentType.SEARCH
                || type == ComponentType.TABLE
                || elementIds.size() >= 4;
    }

    private boolean isSearchElement(PageElementModel element) {
        return containsAny(evidence(element), "search", "filter", "lookup");
    }

    private boolean isNavigationElement(PageElementModel element) {
        String evidence = evidence(element);
        return containsAny(element.technicalType(), "LINK")
                || containsAny(evidence, "navigation", "menu", "tab", "logout", "dashboard", "admin", "pim");
    }

    private boolean isTableElement(PageElementModel element) {
        return containsAny(evidence(element), "table", "row", "column", "grid", "record", "list");
    }

    private String evidence(PageElementModel element) {
        return String.join(" ",
                safe(element.technicalType()),
                safe(element.semanticType()),
                safe(element.tag()),
                safe(element.inputType()),
                safe(element.text()),
                safe(element.id()),
                safe(element.name()),
                safe(element.placeholder()),
                safe(element.ariaLabel()),
                safe(element.role()),
                safe(element.cssClass()),
                String.join(" ", element.attributes().values())
        );
    }

    private String key(PageLocatorModel locator) {
        if (locator == null || locator.strategy().isBlank() || locator.value().isBlank()) {
            return "";
        }
        return locator.strategy().trim().toLowerCase(Locale.ROOT) + "::" + locator.value().trim();
    }

    private String toDisplayName(String value) {
        String semantic = firstNonBlank(value, "component");
        String[] tokens = semantic.split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        if (builder.isEmpty()) {
            return "Component";
        }
        if (!builder.toString().endsWith("Component")) {
            builder.append("Component");
        }
        return builder.toString();
    }

    private String sanitize(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "component" : normalized;
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
        String normalized = safe(text).toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (normalized.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
