package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocatorOriginResolver {

    private static final Pattern ABSOLUTE_URL = Pattern.compile("https?://[^'\"\\]\\)\\s]+", Pattern.CASE_INSENSITIVE);

    public LocatorOrigin resolve(String pageUrl, PageElementModel element, PageLocatorModel locator) {
        String applicationHost = host(pageUrl);
        String href = firstNonBlank(
                element == null ? "" : element.href(),
                extractAbsoluteUrl(locator == null ? "" : locator.value())
        );
        String originHost = host(resolveUrl(pageUrl, href));
        boolean sameOrigin = originHost.isBlank()
                || (!applicationHost.isBlank() && originHost.equalsIgnoreCase(applicationHost));
        return new LocatorOrigin(href, originHost, sameOrigin);
    }

    private String resolveUrl(String pageUrl, String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        try {
            URI hrefUri = URI.create(href.trim());
            if (hrefUri.isAbsolute()) {
                return hrefUri.toString();
            }
            if (pageUrl == null || pageUrl.isBlank()) {
                return href.trim();
            }
            return URI.create(pageUrl.trim()).resolve(hrefUri).toString();
        } catch (IllegalArgumentException exception) {
            return href.trim();
        }
    }

    private String extractAbsoluteUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        Matcher matcher = ABSOLUTE_URL.matcher(value);
        return matcher.find() ? matcher.group() : "";
    }

    private String host(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            String host = URI.create(value.trim()).getHost();
            return Optional.ofNullable(host).orElse("").trim().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record LocatorOrigin(
            String href,
            String originHost,
            boolean sameOrigin
    ) {
        public LocatorOrigin {
            href = href == null ? "" : href.trim();
            originHost = originHost == null ? "" : originHost.trim().toLowerCase(Locale.ROOT);
        }
    }
}
