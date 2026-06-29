package ua.demo.agentlab.ui.discovery.policy;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record DiscoveryCrawlPolicy(
        List<String> startRoutes,
        int maxDepth,
        int maxPages,
        boolean sameDomainOnly,
        boolean followLinks,
        boolean followButtons,
        boolean allowAuthentication,
        boolean allowFormSubmit,
        boolean stopOnLogout,
        List<String> blockedActionKeywords,
        List<String> blockedRouteKeywords
) {
    private static final Pattern ROUTE_PATTERN = Pattern.compile("/[a-zA-Z0-9/_\\-.]+");

    public DiscoveryCrawlPolicy {
        startRoutes = startRoutes == null ? List.of() : List.copyOf(startRoutes);
        maxDepth = Math.max(0, maxDepth);
        maxPages = Math.max(1, maxPages);
        blockedActionKeywords = blockedActionKeywords == null ? List.of() : List.copyOf(blockedActionKeywords);
        blockedRouteKeywords = blockedRouteKeywords == null ? List.of() : List.copyOf(blockedRouteKeywords);
    }

    public static DiscoveryCrawlPolicy defaultPolicy(ProjectProfile projectProfile) {
        Set<String> routes = new LinkedHashSet<>();
        if (projectProfile != null) {
            projectProfile.configuredRoutes().forEach(route -> addConcreteRoute(routes, route));
        }

        return new DiscoveryCrawlPolicy(
                List.copyOf(routes),
                2,
                24,
                true,
                true,
                false,
                true,
                false,
                true,
                List.of("delete", "remove", "pay", "confirm", "close account", "submit order"),
                List.of("logout", "signout", "sign-out", "delete", "remove")
        );
    }

    public List<String> absoluteStartUrls(ProjectProfile projectProfile) {
        return startRoutes.stream()
                .map(route -> toAbsoluteUrl(projectProfile.baseUrl(), route))
                .distinct()
                .toList();
    }

    public List<String> absoluteStartUrls(ProjectProfile projectProfile, NormalizedRequirementBundle requirements) {
        Set<String> routes = new LinkedHashSet<>(startRoutes);
        routes.addAll(extractRequirementRoutes(requirements));
        return routes.stream()
                .map(route -> toAbsoluteUrl(projectProfile.baseUrl(), route))
                .distinct()
                .toList();
    }

    public boolean allowsNavigation(String baseUrl, String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return false;
        }

        String normalized = normalizeUrl(targetUrl);
        String lower = normalized.toLowerCase(Locale.ROOT);

        for (String blockedKeyword : blockedRouteKeywords) {
            if (lower.contains(blockedKeyword.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        if (!sameDomainOnly) {
            return true;
        }

        try {
            URI baseUri = URI.create(baseUrl);
            URI targetUri = URI.create(normalized);
            return baseUri.getHost() != null
                    && targetUri.getHost() != null
                    && baseUri.getHost().equalsIgnoreCase(targetUri.getHost());
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isBlockedAction(String actionLabel) {
        if (actionLabel == null || actionLabel.isBlank()) {
            return false;
        }

        String lower = actionLabel.toLowerCase(Locale.ROOT);
        for (String blockedKeyword : blockedActionKeywords) {
            if (lower.contains(blockedKeyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String toAbsoluteUrl(String baseUrl, String route) {
        if (route == null || route.isBlank()) {
            return baseUrl;
        }
        String normalizedRoute = route.trim();
        if (normalizedRoute.startsWith("http://") || normalizedRoute.startsWith("https://")) {
            return normalizedRoute;
        }
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        if (!normalizedRoute.startsWith("/")) {
            normalizedRoute = "/" + normalizedRoute;
        }
        return normalizedBase + normalizedRoute;
    }

    private static void addConcreteRoute(Set<String> routes, String route) {
        if (route != null && !route.isBlank() && !route.contains("%s")) {
            routes.add(route);
        }
    }

    private List<String> extractRequirementRoutes(NormalizedRequirementBundle requirements) {
        if (requirements == null || requirements.requirements() == null) {
            return List.of();
        }
        Set<String> routes = new LinkedHashSet<>();
        for (NormalizedRequirement requirement : requirements.requirements()) {
            String text = String.join(" ",
                    requirement.title(),
                    requirement.statement(),
                    requirement.expectedResult(),
                    String.join(" ", requirement.tags()));
            Matcher matcher = ROUTE_PATTERN.matcher(text);
            while (matcher.find()) {
                addConcreteRoute(routes, matcher.group());
            }
        }
        return List.copyOf(routes);
    }

    private String normalizeUrl(String value) {
        try {
            URI uri = URI.create(value);
            URI normalized = new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    null,
                    null
            );
            return normalized.toString();
        } catch (Exception exception) {
            return value;
        }
    }
}
