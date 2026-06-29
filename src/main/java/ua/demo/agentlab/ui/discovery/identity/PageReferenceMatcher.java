package ua.demo.agentlab.ui.discovery.identity;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.flow.model.CanonicalPage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class PageReferenceMatcher {

    private PageReferenceMatcher() {
    }

    public static boolean matches(MappedPage page, String reference) {
        if (page == null) {
            return false;
        }
        return matches(
                page.pageIdentity(),
                page.canonicalPageType(),
                page.pageName(),
                page.urlPattern(),
                page.url(),
                page.title(),
                page.pageType(),
                reference
        );
    }

    public static boolean matches(CanonicalPage page, String reference) {
        if (page == null) {
            return false;
        }
        return matches(
                page.pageIdentity(),
                page.canonicalPageType(),
                page.pageName(),
                page.route(),
                page.route(),
                page.source(),
                "",
                reference
        );
    }

    public static boolean matches(DiscoveredUiPage page, String reference) {
        if (page == null) {
            return false;
        }
        return matches(
                page.pageIdentity(),
                page.canonicalPageType(),
                page.pageName(),
                page.route(),
                page.route(),
                page.discoveryReason(),
                "",
                reference
        );
    }

    public static boolean matchesScenarioPage(String pageName, String route, String reference) {
        return matches(null, null, pageName, route, route, "", "", reference);
    }

    public static boolean matchesAny(MappedPage page, Iterable<String> references) {
        return matchesAny(
                page == null ? null : page.pageIdentity(),
                page == null ? null : page.canonicalPageType(),
                page == null ? "" : page.pageName(),
                page == null ? "" : page.urlPattern(),
                page == null ? "" : page.url(),
                page == null ? "" : page.title(),
                page == null ? "" : page.pageType(),
                references
        );
    }

    public static boolean matchesAny(CanonicalPage page, Iterable<String> references) {
        return matchesAny(
                page == null ? null : page.pageIdentity(),
                page == null ? null : page.canonicalPageType(),
                page == null ? "" : page.pageName(),
                page == null ? "" : page.route(),
                page == null ? "" : page.route(),
                page == null ? "" : page.source(),
                "",
                references
        );
    }

    public static boolean matches(
            PageIdentity identity,
            CanonicalPageType canonicalPageType,
            String pageName,
            String route,
            String url,
            String title,
            String pageType,
            String reference
    ) {
        String normalizedReference = normalize(reference);
        if (normalizedReference.isBlank()) {
            return false;
        }
        if (identity != null && identity.matches(reference)) {
            return true;
        }
        if (collectReferences(identity, canonicalPageType, pageName, route, url, title, pageType).contains(normalizedReference)) {
            return true;
        }
        return routeMatches(route, reference)
                || routeMatches(url, reference)
                || identity != null && routeMatches(identity.routePattern(), reference);
    }

    public static boolean matchesAny(
            PageIdentity identity,
            CanonicalPageType canonicalPageType,
            String pageName,
            String route,
            String url,
            String title,
            String pageType,
            Iterable<String> references
    ) {
        if (references == null) {
            return false;
        }
        for (String reference : references) {
            if (matches(identity, canonicalPageType, pageName, route, url, title, pageType, reference)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> collectReferences(MappedPage page) {
        if (page == null) {
            return Set.of();
        }
        return collectReferences(
                page.pageIdentity(),
                page.canonicalPageType(),
                page.pageName(),
                page.urlPattern(),
                page.url(),
                page.title(),
                page.pageType()
        );
    }

    public static Set<String> collectReferences(CanonicalPage page) {
        if (page == null) {
            return Set.of();
        }
        return collectReferences(
                page.pageIdentity(),
                page.canonicalPageType(),
                page.pageName(),
                page.route(),
                page.route(),
                page.source(),
                ""
        );
    }

    public static Set<String> collectReferences(String pageName, String route) {
        return collectReferences(null, null, pageName, route, route, "", "");
    }

    public static Set<String> collectReferences(
            PageIdentity identity,
            CanonicalPageType canonicalPageType,
            String pageName,
            String route,
            String url,
            String title,
            String pageType
    ) {
        Set<String> references = new LinkedHashSet<>();
        addReference(references, pageName);
        addReference(references, route);
        addReference(references, url);
        addReference(references, title);
        addReference(references, pageType);
        if (canonicalPageType != null) {
            addReference(references, canonicalPageType.name());
            addReference(references, canonicalPageType.defaultAlias());
            addReference(references, canonicalPageType.mappedType());
        }
        if (identity != null) {
            addReference(references, identity.key());
            addReference(references, identity.className());
            addReference(references, identity.displayName());
            addReference(references, identity.routePattern());
            identity.aliases().forEach(alias -> addReference(references, alias));
        }
        return references;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    public static boolean routeMatches(String candidateRoute, String referenceRoute) {
        String candidate = normalizeRoute(candidateRoute);
        String reference = normalizeRoute(referenceRoute);
        if (candidate.isBlank() || reference.isBlank()) {
            return false;
        }
        if (candidate.equals(reference) || RouteCanonicalizer.routeEqualsOrSuffix(candidateRoute, referenceRoute)) {
            return true;
        }
        String candidatePrefix = normalizeRoutePrefix(candidateRoute);
        String referencePrefix = normalizeRoutePrefix(referenceRoute);
        if (routePrefixMatches(referencePrefix, candidate)
                || routePrefixMatches(candidatePrefix, reference)
                || routePrefixMatches(referencePrefix, candidatePrefix)
                || routePrefixMatches(candidatePrefix, referencePrefix)) {
            return true;
        }
        return routePatternMatches(reference, candidate) || routePatternMatches(candidate, reference);
    }

    private static boolean routePrefixMatches(String referenceRoute, String candidateRoute) {
        if (referenceRoute == null || candidateRoute == null
                || referenceRoute.isBlank() || candidateRoute.isBlank()) {
            return false;
        }
        if (!referenceRoute.endsWith("/") || "/".equals(referenceRoute)) {
            return false;
        }
        return candidateRoute.startsWith(referenceRoute)
                || candidateRoute.contains(referenceRoute);
    }

    private static boolean routePatternMatches(String patternRoute, String concreteRoute) {
        if (!isPatternRoute(patternRoute)) {
            return false;
        }
        String[] patternSegments = patternRoute.split("/");
        String[] concreteSegments = concreteRoute.split("/");
        if (patternSegments.length != concreteSegments.length) {
            return false;
        }
        for (int index = 0; index < patternSegments.length; index++) {
            String patternSegment = patternSegments[index];
            String concreteSegment = concreteSegments[index];
            if (patternSegment.isBlank() && concreteSegment.isBlank()) {
                continue;
            }
            if (isWildcardSegment(patternSegment)) {
                if (concreteSegment.isBlank()) {
                    return false;
                }
                continue;
            }
            String regex = wildcardRegex(patternSegment);
            if (regex == null) {
                if (!patternSegment.equals(concreteSegment)) {
                    return false;
                }
                continue;
            }
            if (!Pattern.matches(regex, concreteSegment)) {
                return false;
            }
        }
        return true;
    }

    private static String wildcardRegex(String segment) {
        if (!segment.contains("%s") && !segment.contains("*")) {
            return null;
        }
        String regex = Pattern.quote(segment)
                .replace("%s", "\\E[^/]+\\Q")
                .replace("*", "\\E[^/]+\\Q");
        return regex;
    }

    private static boolean isPatternRoute(String route) {
        return route.contains("%s")
                || route.contains("*")
                || route.matches(".*/\\{[^/]+}.*")
                || route.matches(".*/:[^/]+.*");
    }

    private static boolean isWildcardSegment(String segment) {
        return "%s".equals(segment)
                || "*".equals(segment)
                || segment.matches("\\{[^/]+}")
                || segment.startsWith(":");
    }

    private static String normalizeRoute(String value) {
        return RouteCanonicalizer.canonicalize(value);
    }

    private static String normalizeRoutePrefix(String value) {
        String normalized = RouteCanonicalizer.canonicalize(value);
        if (!normalized.endsWith("/") && isPrefixLike(value)) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private static boolean isPrefixLike(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.endsWith("/") && trimmed.length() > 1) {
            return true;
        }
        return trimmed.endsWith("/*")
                || trimmed.endsWith("/{id}")
                || trimmed.endsWith("/:id")
                || trimmed.endsWith("/%s");
    }

    private static void addReference(Set<String> references, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            references.add(normalized);
        }
    }
}
