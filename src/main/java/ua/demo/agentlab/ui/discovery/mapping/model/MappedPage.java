package ua.demo.agentlab.ui.discovery.mapping.model;

import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;
import ua.demo.agentlab.ui.discovery.identity.PageIdentity;

import java.util.List;

public record MappedPage(
        String pageId,
        String pageName,
        String pageType,
        String url,
        String urlPattern,
        String title,
        List<MappedSection> sections,
        List<MappedElement> elements,
        List<MappedForm> forms,
        List<MappedAction> actions,
        List<AssertionHint> assertionHints,
        PageStateHints stateHints,
        String screenshotPath,
        String htmlPath,
        CanonicalPageType canonicalPageType,
        PageIdentity pageIdentity
) {
    public MappedPage(
            String pageId,
            String pageName,
            String pageType,
            String url,
            String urlPattern,
            String title,
            List<MappedSection> sections,
            List<MappedElement> elements,
            List<MappedForm> forms,
            List<MappedAction> actions,
            List<AssertionHint> assertionHints,
            PageStateHints stateHints,
            String screenshotPath,
            String htmlPath
    ) {
        this(
                pageId,
                pageName,
                pageType,
                url,
                urlPattern,
                title,
                sections,
                elements,
                forms,
                actions,
                assertionHints,
                stateHints,
                screenshotPath,
                htmlPath,
                CanonicalPageType.fromMappedType(pageType),
                PageIdentity.legacy(pageName, pageType, urlPattern)
        );
    }

    public MappedPage {
        pageId = pageId == null ? "" : pageId.trim();
        pageName = pageName == null ? "" : pageName.trim();
        pageType = pageType == null ? "" : pageType.trim();
        url = url == null ? "" : url.trim();
        urlPattern = urlPattern == null ? "" : urlPattern.trim();
        title = title == null ? "" : title.trim();
        sections = sections == null ? List.of() : List.copyOf(sections);
        elements = elements == null ? List.of() : List.copyOf(elements);
        forms = forms == null ? List.of() : List.copyOf(forms);
        actions = actions == null ? List.of() : List.copyOf(actions);
        assertionHints = assertionHints == null ? List.of() : List.copyOf(assertionHints);
        stateHints = stateHints == null ? new PageStateHints(false, false, false, false, false, false) : stateHints;
        screenshotPath = screenshotPath == null ? "" : screenshotPath.trim();
        htmlPath = htmlPath == null ? "" : htmlPath.trim();
        canonicalPageType = canonicalPageType == null ? CanonicalPageType.fromMappedType(pageType) : canonicalPageType;
        pageIdentity = pageIdentity == null ? PageIdentity.legacy(pageName, pageType, urlPattern) : pageIdentity;
    }
}
