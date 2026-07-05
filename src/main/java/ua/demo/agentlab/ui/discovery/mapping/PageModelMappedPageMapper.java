package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;
import ua.demo.agentlab.ui.discovery.identity.PageIdentity;
import ua.demo.agentlab.ui.discovery.mapping.model.AssertionHint;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedSection;
import ua.demo.agentlab.ui.discovery.mapping.model.PageStateHints;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageApiRelationModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PageModelMappedPageMapper {

    private final PageModelElementMapper elementMapper;
    private final PageModelFormMapper formMapper;

    public PageModelMappedPageMapper() {
        this(new PageModelElementMapper(), new PageModelFormMapper());
    }

    public PageModelMappedPageMapper(PageModelElementMapper elementMapper, PageModelFormMapper formMapper) {
        this.elementMapper = elementMapper;
        this.formMapper = formMapper;
    }

    public MappedPage map(PageModel page) {
        List<MappedElement> elements = page.elements().stream()
                .map(element -> elementMapper.map(page.url(), element))
                .toList();
        Map<String, PageElementModel> elementsById = mapElementsById(page.elements());
        List<MappedForm> forms = page.forms().stream()
                .map(form -> formMapper.map(page.url(), form, elementsById))
                .toList();
        List<MappedAction> actions = new ArrayList<>(page.elements().stream()
                .flatMap(element -> elementMapper.mapActions(page.pageId(), element).stream())
                .distinct()
                .toList());
        actions.addAll(page.apiRelations().stream()
                .map(relation -> toRuntimeAction(page.pageId(), relation))
                .toList());

        CanonicalPageType canonicalPageType = CanonicalPageType.fromMappedType(page.featureGuess());
        String pageName = canonicalPageType == CanonicalPageType.GENERIC
                ? toPageName(page)
                : canonicalPageType.defaultClassName();
        String pageType = canonicalPageType.mappedType();

        return new MappedPage(
                page.pageId(),
                pageName,
                pageType,
                page.url(),
                page.route(),
                page.title(),
                buildSections(page, elements, forms),
                elements,
                forms,
                actions,
                buildAssertionHints(page, pageName),
                buildStateHints(page, forms),
                page.evidence().screenshotPath(),
                page.evidence().htmlPath(),
                canonicalPageType,
                PageIdentity.legacy(pageName, pageType, page.route())
        );
    }

    private MappedAction toRuntimeAction(String pageId, PageApiRelationModel relation) {
        String actionType = relation.relationType().isBlank() ? "runtime-api" : relation.relationType();
        String endpoint = relation.endpoint().isBlank() ? "runtime endpoint" : relation.endpoint();
        return new MappedAction(
                pageId + ":runtime:" + sanitize(actionType + "-" + endpoint),
                toActionName(actionType),
                actionType,
                relation.elementId(),
                "",
                "Runtime API relation " + endpoint + " (" + relation.reason() + ")",
                relation.confidenceScore()
        );
    }

    private String toActionName(String actionType) {
        String normalized = actionType == null ? "" : actionType.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "Observe runtime API";
        }
        String[] parts = normalized.split("[^a-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? "Observe runtime API" : builder.toString();
    }

    private List<MappedSection> buildSections(PageModel page, List<MappedElement> elements, List<MappedForm> forms) {
        if (!forms.isEmpty()) {
            return List.of(
                    new MappedSection(
                            page.pageId() + ":section:form",
                            "FormSection",
                            "form",
                            elements.stream().map(MappedElement::elementId).toList()
                    )
            );
        }
        return List.of(new MappedSection(
                page.pageId() + ":section:content",
                "ContentSection",
                "content",
                elements.stream().map(MappedElement::elementId).toList()
        ));
    }

    private List<AssertionHint> buildAssertionHints(PageModel page, String pageName) {
        return List.of(
                new AssertionHint("url-contains", page.route(), "Route evidence for " + pageName, 0.80d),
                new AssertionHint("page-visible", pageName, "Page-specific visible content exists", 0.65d)
        );
    }

    private PageStateHints buildStateHints(PageModel page, List<MappedForm> forms) {
        String text = normalize(page.featureGuess() + " " + page.visibleText() + " " + page.route());
        boolean authenticated = text.contains("authenticated")
                || text.contains("secure")
                || text.contains("logout")
                || text.contains("dashboard")
                || text.contains("/dashboard");
        return new PageStateHints(
                !authenticated,
                authenticated,
                text.contains("listing") || text.contains("collection"),
                text.contains("details"),
                !forms.isEmpty(),
                text.contains("error") || text.contains("invalid") || text.contains("alert")
        );
    }

    private Map<String, PageElementModel> mapElementsById(List<PageElementModel> elements) {
        Map<String, PageElementModel> mapped = new LinkedHashMap<>();
        for (PageElementModel element : elements) {
            mapped.put(element.elementId(), element);
        }
        return mapped;
    }

    private String toPageName(PageModel page) {
        String token = page.route() == null || page.route().isBlank() || "/".equals(page.route())
                ? firstNonBlank(page.pageId(), "Home")
                : page.route().substring(page.route().lastIndexOf('/') + 1);
        String[] parts = token.split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        if (builder.isEmpty()) {
            builder.append("Generic");
        }
        if (!builder.toString().endsWith("Page")) {
            builder.append("Page");
        }
        return builder.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String sanitize(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "runtime" : normalized;
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
