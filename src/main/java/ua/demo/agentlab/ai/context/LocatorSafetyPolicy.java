package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.net.URI;
import java.util.Locale;

public class LocatorSafetyPolicy {

    public boolean componentPromptSafeLocator(
            ScopedLocatorCandidate candidate,
            PageElementModel element,
            PageLocatorModel pageLocator,
            MappedPage targetPage
    ) {
        if (candidate == null || candidate.value().isBlank() || element == null || !element.visible()) {
            return false;
        }
        if (!candidate.uniqueWithinComponent() || candidate.finalScore() < 0.72d) {
            return false;
        }
        if (candidate.globalMatchCount() < 0 || candidate.scopedMatchCount() < 0) {
            return false;
        }
        if (candidate.risks().stream().anyMatch(this::componentForbiddenRisk)) {
            return false;
        }
        String evidence = elementEvidence(element, candidate.value());
        if (containsAny(evidence, "csrf", "xsrf", "token", "_token", "authenticity_token")) {
            return false;
        }
        return promptSafePageModelLocator(element, pageLocator, targetPage)
                || candidate.uniqueWithinComponent()
                && !externalHref(element.href(), targetPage)
                && !externalLocatorValue(candidate.value(), targetPage);
    }

    public boolean promptSafePageModelLocator(PageElementModel element, PageLocatorModel locator, MappedPage targetPage) {
        if (locator == null || locator.value().isBlank() || element == null) {
            return false;
        }
        if (!locator.stableAcrossRuns() || locator.score() < 0.75d) {
            return false;
        }
        if (locator.browserMatchCount() < 0 || locator.browserScopedMatchCount() < 0 || !locator.unique()) {
            return false;
        }
        String value = locator.value().toLowerCase(Locale.ROOT);
        String evidence = elementEvidence(element, locator.reason());
        if (containsAny(evidence, "csrf", "xsrf", "token", "_token", "authenticity_token")) {
            return false;
        }
        if (containsAny(value, "/html[", "body/", "following-sibling", "preceding-sibling")) {
            return false;
        }
        if (externalHref(element.href(), targetPage) || externalLocatorValue(locator.value(), targetPage)) {
            return false;
        }
        return !containsAny(evidence, "external-origin", "hidden-or-invisible", "security-token", "semantic-locator-conflict");
    }

    public boolean componentForbiddenRisk(String risk) {
        String normalized = risk == null ? "" : risk.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("dynamic-css-hash")
                || normalized.equals("nth-child-selector")
                || normalized.equals("absolute-dom-path")
                || normalized.equals("deep-dom-chain")
                || normalized.equals("framework-generated-class")
                || normalized.equals("security-token-field")
                || normalized.equals("hidden-or-invisible-element")
                || normalized.equals("browser-global-count-missing")
                || normalized.equals("browser-scoped-count-missing")
                || normalized.equals("not-component-unique");
    }

    public boolean externalHref(String href, MappedPage targetPage) {
        String normalized = href == null ? "" : href.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("//")) {
            return true;
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            return false;
        }
        return !sameOrigin(normalized, targetPage.url());
    }

    public boolean externalLocatorValue(String value, MappedPage targetPage) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("//") && !normalized.contains("http://") && !normalized.contains("https://")) {
            return true;
        }
        String absoluteUrl = extractAbsoluteUrl(normalized);
        return !absoluteUrl.isBlank() && !sameOrigin(absoluteUrl, targetPage.url());
    }

    public boolean containsAny(String value, String... fragments) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String elementEvidence(PageElementModel element, String extra) {
        return String.join(" ",
                element.elementId(),
                element.semanticType(),
                element.technicalType(),
                element.tag(),
                element.inputType(),
                element.name(),
                element.id(),
                element.placeholder(),
                element.ariaLabel(),
                element.role(),
                element.href(),
                element.text(),
                extra == null ? "" : extra
        ).toLowerCase(Locale.ROOT);
    }

    private String extractAbsoluteUrl(String value) {
        int http = value.indexOf("http://");
        int https = value.indexOf("https://");
        int start = http >= 0 ? http : https;
        if (start < 0) {
            return "";
        }
        int end = value.length();
        for (int index = start; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\'' || ch == '"' || Character.isWhitespace(ch) || ch == ']') {
                end = index;
                break;
            }
        }
        return value.substring(start, end);
    }

    private boolean sameOrigin(String candidateUrl, String targetUrl) {
        try {
            URI candidate = URI.create(candidateUrl);
            URI target = URI.create(targetUrl == null ? "" : targetUrl.trim());
            return candidate.getScheme() != null
                    && target.getScheme() != null
                    && candidate.getScheme().equalsIgnoreCase(target.getScheme())
                    && safe(candidate.getHost()).equalsIgnoreCase(safe(target.getHost()))
                    && port(candidate) == port(target);
        } catch (Exception ignored) {
            return false;
        }
    }

    private int port(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme.equals("https")) {
            return 443;
        }
        if (scheme.equals("http")) {
            return 80;
        }
        return -1;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
