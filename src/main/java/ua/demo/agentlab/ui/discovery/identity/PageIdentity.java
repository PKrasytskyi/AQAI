package ua.demo.agentlab.ui.discovery.identity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record PageIdentity(
        String key,
        CanonicalPageType canonicalPageType,
        String className,
        String displayName,
        String routePattern,
        List<String> aliases
) {
    public PageIdentity {
        key = safe(key);
        canonicalPageType = canonicalPageType == null ? CanonicalPageType.GENERIC : canonicalPageType;
        className = safe(className);
        displayName = safe(displayName);
        routePattern = safe(routePattern);
        aliases = aliases == null ? List.of() : List.copyOf(new LinkedHashSet<>(aliases.stream()
                .map(PageIdentity::safe)
                .filter(value -> !value.isBlank())
                .toList()));
    }

    public static PageIdentity legacy(String pageName, String pageType, String routePattern) {
        CanonicalPageType canonicalPageType = pageType == null || pageType.isBlank()
                ? CanonicalPageType.fromLegacyPageName(pageName)
                : CanonicalPageType.fromMappedType(pageType);
        String resolvedClassName = safe(pageName).isBlank() ? canonicalPageType.defaultClassName() : pageName.trim();
        String resolvedRoute = safe(routePattern);
        String displayName = resolvedClassName.endsWith("Page")
                ? resolvedClassName.substring(0, resolvedClassName.length() - 4)
                : resolvedClassName;
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(canonicalPageType.defaultAlias());
        aliases.add(normalize(displayName));
        if (!resolvedRoute.isBlank() && !"/".equals(resolvedRoute)) {
            aliases.add(normalize(lastRouteToken(resolvedRoute)));
        }
        return new PageIdentity(
                buildKey(canonicalPageType, resolvedRoute, resolvedClassName),
                canonicalPageType,
                resolvedClassName,
                humanize(displayName),
                resolvedRoute,
                List.copyOf(aliases)
        );
    }

    public boolean matches(String candidate) {
        String normalized = normalize(candidate);
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.equals(normalize(key))
                || normalized.equals(normalize(className))
                || normalized.equals(normalize(displayName))
                || normalized.equals(normalize(routePattern))
                || aliases.stream().map(PageIdentity::normalize).anyMatch(normalized::equals);
    }

    public static String buildKey(CanonicalPageType canonicalPageType, String routePattern, String fallbackName) {
        String routeToken = normalize(lastRouteToken(routePattern));
        if (!routeToken.isBlank() && !"page".equals(routeToken) && !"index".equals(routeToken)) {
            return canonicalPageType.name().toLowerCase(Locale.ROOT) + ":" + routeToken;
        }
        String fallback = normalize(fallbackName);
        if (!fallback.isBlank()) {
            return canonicalPageType.name().toLowerCase(Locale.ROOT) + ":" + fallback;
        }
        return canonicalPageType.name().toLowerCase(Locale.ROOT);
    }

    public static String lastRouteToken(String routePattern) {
        String sanitized = safe(routePattern);
        if (sanitized.isBlank() || "/".equals(sanitized)) {
            return "";
        }
        String[] tokens = sanitized.split("/");
        for (int index = tokens.length - 1; index >= 0; index--) {
            String token = normalize(tokens[index]);
            if (!token.isBlank() && !"products".equals(token) && !"collections".equals(token)) {
                return tokens[index];
            }
        }
        return tokens[tokens.length - 1];
    }

    public static String humanize(String value) {
        String normalized = safe(value).replaceAll("([a-z])([A-Z])", "$1 $2");
        String[] tokens = normalized.split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.length() == 0 ? "Page" : builder.toString();
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
