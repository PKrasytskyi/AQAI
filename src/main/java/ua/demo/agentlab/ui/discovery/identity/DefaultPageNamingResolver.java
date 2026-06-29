package ua.demo.agentlab.ui.discovery.identity;

import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DefaultPageNamingResolver implements PageNamingResolver {

    private static final Set<String> GENERIC_ROUTE_TOKENS = Set.of(
            "index", "home", "page", "pages", "view", "app", "default", "products", "collections"
    );

    private final AliasRegistry aliasRegistry;

    public DefaultPageNamingResolver(AliasRegistry aliasRegistry) {
        if (aliasRegistry == null) {
            throw new IllegalArgumentException("aliasRegistry cannot be null");
        }
        this.aliasRegistry = aliasRegistry;
    }

    @Override
    public PageIdentity resolve(
            CanonicalPageType canonicalPageType,
            String route,
            String title,
            List<String> headings,
            List<String> capabilities,
            List<DiscoveredUiPage> knownPages
    ) {
        List<String> aliases = aliasRegistry.aliasesFor(canonicalPageType, route, title, headings, capabilities);
        String displayName = resolveDisplayName(route, title, headings, canonicalPageType);
        String preferredBaseName = resolvePreferredBaseName(route, title, headings, aliases, canonicalPageType);
        String className = ensureUniqueClassName(toTypeName(preferredBaseName) + "Page", route, knownPages, canonicalPageType);

        return new PageIdentity(
                PageIdentity.buildKey(canonicalPageType, route, className),
                canonicalPageType,
                className,
                displayName,
                route == null ? "" : route.trim(),
                aliases
        );
    }

    private String resolveDisplayName(String route, String title, List<String> headings, CanonicalPageType canonicalPageType) {
        String candidate = firstMeaningful(headings);
        if (candidate.isBlank()) {
            candidate = firstMeaningful(title);
        }
        if (candidate.isBlank()) {
            candidate = PageIdentity.lastRouteToken(route);
        }
        if (candidate.isBlank()) {
            candidate = canonicalPageType.defaultAlias();
        }
        return PageIdentity.humanize(candidate);
    }

    private String resolvePreferredBaseName(
            String route,
            String title,
            List<String> headings,
            List<String> aliases,
            CanonicalPageType canonicalPageType
    ) {
        String routeToken = normalizeToken(PageIdentity.lastRouteToken(route));
        if (!routeToken.isBlank() && !GENERIC_ROUTE_TOKENS.contains(routeToken)) {
            return routeToken;
        }

        String headingToken = normalizeToken(firstMeaningful(headings));
        if (!headingToken.isBlank() && !isTooGeneric(headingToken, canonicalPageType)) {
            return headingToken;
        }

        String titleToken = normalizeToken(firstMeaningful(title));
        if (!titleToken.isBlank() && !isTooGeneric(titleToken, canonicalPageType)) {
            return titleToken;
        }

        for (String alias : aliases) {
            String token = normalizeToken(alias);
            if (!token.isBlank() && !isTooGeneric(token, canonicalPageType)) {
                return token;
            }
        }

        return canonicalPageType.defaultAlias();
    }

    private String ensureUniqueClassName(
            String candidate,
            String route,
            List<DiscoveredUiPage> knownPages,
            CanonicalPageType canonicalPageType
    ) {
        if (knownPages == null || knownPages.isEmpty()) {
            return fallbackIfGeneric(candidate, canonicalPageType);
        }
        String normalizedCandidate = normalizeClassName(candidate);
        boolean conflict = knownPages.stream()
                .map(DiscoveredUiPage::pageIdentity)
                .filter(identity -> identity != null && !identity.routePattern().equalsIgnoreCase(safe(route)))
                .map(PageIdentity::className)
                .map(this::normalizeClassName)
                .anyMatch(normalizedCandidate::equals);
        if (!conflict) {
            return fallbackIfGeneric(candidate, canonicalPageType);
        }
        String routeToken = normalizeToken(PageIdentity.lastRouteToken(route));
        if (!routeToken.isBlank()) {
            return toTypeName(routeToken) + canonicalPageType.defaultClassName();
        }
        return canonicalPageType.defaultClassName();
    }

    private String fallbackIfGeneric(String candidate, CanonicalPageType canonicalPageType) {
        String normalized = normalizeClassName(candidate);
        if (normalized.equalsIgnoreCase("Page") || normalized.equalsIgnoreCase("GenericPage")) {
            return canonicalPageType.defaultClassName();
        }
        return candidate;
    }

    private boolean isTooGeneric(String token, CanonicalPageType canonicalPageType) {
        return token.isBlank()
                || GENERIC_ROUTE_TOKENS.contains(token)
                || token.equals(canonicalPageType.defaultAlias())
                || token.equals("generic");
    }

    private String firstMeaningful(List<String> values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = firstMeaningful(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String firstMeaningful(String value) {
        String normalized = safe(value)
                .replaceAll("[|:]+", " ")
                .replaceAll("[-_]+", " ")
                .trim();
        if (normalized.isBlank()) {
            return "";
        }
        return normalized;
    }

    private String toTypeName(String value) {
        String[] tokens = safe(value).split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.length() == 0 ? "Generic" : builder.toString();
    }

    private String normalizeToken(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String normalizeClassName(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
