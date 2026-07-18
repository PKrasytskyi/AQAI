package ua.demo.agentlab.ui.catalog;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.StructuredRequirementContext;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfirmedPageSourceResolver {

    private static final Pattern ROUTE_PATTERN = Pattern.compile("(?<![A-Za-z0-9])/[a-zA-Z0-9][a-zA-Z0-9/_\\-.]*");
    private final Neo4jStableCapabilityLookupService stableCapabilityLookup;

    public ConfirmedPageSourceResolver() { this(new Neo4jStableCapabilityLookupService()); }
    public ConfirmedPageSourceResolver(boolean includeStableCache) {
        this(includeStableCache ? new Neo4jStableCapabilityLookupService() : null);
    }
    ConfirmedPageSourceResolver(Neo4jStableCapabilityLookupService stableCapabilityLookup) {
        this.stableCapabilityLookup = stableCapabilityLookup;
    }

    public ConfirmedPageRegistry resolve(ProjectProfile profile) {
        return resolve(profile, null, List.of());
    }

    public ConfirmedPageRegistry resolve(
            ProjectProfile profile,
            NormalizedRequirementBundle requirements,
            List<ConfirmedPageCandidate> stableCachedPages
    ) {
        return resolve(profile, requirements, null, stableCachedPages);
    }

    public ConfirmedPageRegistry resolve(
            ProjectProfile profile,
            NormalizedRequirementBundle requirements,
            UiDiscoverySnapshot discoverySnapshot,
            List<ConfirmedPageCandidate> stableCachedPages
    ) {
        List<ConfirmedPageCandidate> candidates = new ArrayList<>();
        addProfileRoutes(candidates, profile);
        addRequirementRoutes(candidates, requirements);
        addDiscoveryRoutes(candidates, discoverySnapshot);
        if (stableCapabilityLookup != null) {
            candidates.addAll(stableCapabilityLookup.findStablePages(profile));
        }
        if (stableCachedPages != null) {
            stableCachedPages.stream()
                    .filter(candidate -> candidate != null && candidate.hasRoute())
                    .forEach(candidates::add);
        }
        return new ConfirmedPageRegistry(candidates);
    }

    private void addProfileRoutes(List<ConfirmedPageCandidate> candidates, ProjectProfile profile) {
        if (profile == null) {
            return;
        }
        addCandidate(candidates, profile.homeRoute(), PageCapability.NAVIGATION, PageSource.EXPLICIT_PROFILE,
                "project.route.home");
        addCandidate(candidates, profile.loginRoute(), PageCapability.AUTHENTICATION, PageSource.EXPLICIT_PROFILE,
                "project.route.login");
        addCandidate(candidates, profile.registrationRoute(), PageCapability.REGISTRATION, PageSource.EXPLICIT_PROFILE,
                "project.route.registration");
        addCandidate(candidates, profile.authenticatedRoute(), authenticatedCapability(profile.authenticatedRoute()),
                PageSource.EXPLICIT_PROFILE, "project.route.authenticated");
        addCandidate(candidates, profile.recoveryRoute(), PageCapability.RECOVERY, PageSource.EXPLICIT_PROFILE,
                "project.route.recovery");
        addCandidate(candidates, profile.detailsRoute(), PageCapability.RECORD_DETAILS, PageSource.EXPLICIT_PROFILE,
                "project.route.details");
        addCandidate(candidates, profile.formRoute(), PageCapability.FORM, PageSource.EXPLICIT_PROFILE,
                "project.route.form");
        addCandidate(candidates, profile.securityRoute(), PageCapability.SECURITY, PageSource.EXPLICIT_PROFILE,
                "project.route.security");
        addCandidate(candidates, profile.catalogRoute(), PageCapability.RECORD_LIST, PageSource.EXPLICIT_PROFILE,
                "project.route.catalog");
        addCandidate(candidates, profile.productsRoute(), PageCapability.RECORD_DETAILS, PageSource.EXPLICIT_PROFILE,
                "project.route.products");
        addCandidate(candidates, profile.cartRoute(), PageCapability.CONTAINER, PageSource.EXPLICIT_PROFILE,
                "project.route.cart");
    }

    private void addRequirementRoutes(List<ConfirmedPageCandidate> candidates, NormalizedRequirementBundle requirements) {
        if (requirements == null || requirements.requirements() == null) {
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (NormalizedRequirement requirement : requirements.requirements()) {
            String structuredRoute = StructuredRequirementContext.targetRoute(requirement);
            if (!structuredRoute.isBlank() && seen.add(structuredRoute)) {
                String capabilityText = StructuredRequirementContext.pageCapability(requirement);
                addCandidate(candidates, structuredRoute,
                        inferCapability(capabilityText + " " + requirement.title()),
                        PageSource.REQUIREMENT_ROUTE,
                        requirement.id() + " " + sourceLine(requirement));
            }
            String text = requirementText(requirement);
            Matcher matcher = ROUTE_PATTERN.matcher(text);
            while (matcher.find()) {
                String route = matcher.group();
                if (!isExplicitRequirementRoute(text, matcher.start(), route)) {
                    continue;
                }
                if (seen.add(route)) {
                    PageCapability capability = inferCapability(route + " " + text);
                    addCandidate(
                            candidates,
                            route,
                            capability,
                            PageSource.REQUIREMENT_ROUTE,
                            requirement.id() + " " + sourceLine(requirement)
                    );
                }
            }
        }
    }

    private void addDiscoveryRoutes(List<ConfirmedPageCandidate> candidates, UiDiscoverySnapshot discoverySnapshot) {
        if (discoverySnapshot == null || discoverySnapshot.pages() == null) {
            return;
        }
        for (DiscoveredUiPage page : discoverySnapshot.pages()) {
            if (page == null || page.route() == null || page.route().isBlank()) {
                continue;
            }
            PageCapability capability = inferCapability(page.pageName()
                    + " " + page.route()
                    + " " + String.join(" ", page.capabilities() == null ? List.of() : page.capabilities())
                    + " " + page.discoveryReason());
            candidates.add(new ConfirmedPageCandidate(
                    page.pageName() == null || page.pageName().isBlank()
                            ? resolvedPageName(page.route(), capability)
                            : page.pageName(),
                    page.route(),
                    capability,
                    PageSource.DISCOVERY_SNAPSHOT,
                    0.88d,
                    List.of("ui-discovery-snapshot")
            ));
        }
    }

    private void addCandidate(
            List<ConfirmedPageCandidate> candidates,
            String route,
            PageCapability capability,
            PageSource source,
            String evidence
    ) {
        if (route == null || route.isBlank()) {
            return;
        }
        PageCapability resolvedCapability = capability == null ? PageCapability.GENERIC : capability;
        candidates.add(new ConfirmedPageCandidate(
                resolvedPageName(route, resolvedCapability),
                route,
                resolvedCapability,
                source,
                source == PageSource.EXPLICIT_PROFILE ? 0.98d : 0.82d,
                List.of(evidence)
        ));
    }

    private PageCapability authenticatedCapability(String route) {
        String normalized = normalize(route);
        if (normalized.contains("dashboard") || normalized.contains("overview")) {
            return PageCapability.DASHBOARD;
        }
        return PageCapability.AUTHENTICATED_AREA;
    }

    private PageCapability inferCapability(String text) {
        String normalized = normalize(text);
        if (containsAny(normalized, "dashboard", "overview")) {
            return PageCapability.DASHBOARD;
        }
        if (containsAny(normalized, "/profile", "account/profile", "account / profile", "account page", "profile page")) {
            return PageCapability.RECORD_DETAILS;
        }
        if (containsAny(normalized, "secure", "authenticated", "logout")) {
            return PageCapability.AUTHENTICATED_AREA;
        }
        if (containsAny(normalized, "register", "registration", "signup", "sign-up")) {
            return PageCapability.REGISTRATION;
        }
        if (containsAny(normalized, "recover", "recovery", "forgot", "reset")) {
            return PageCapability.RECOVERY;
        }
        if (containsAny(normalized, "security", "challenge", "mfa", "otp")) {
            return PageCapability.SECURITY;
        }
        if (containsAny(normalized, "login", "signin", "sign-in", "auth", "credential")) {
            return PageCapability.AUTHENTICATION;
        }
        if (containsAny(normalized, "cart", "basket", "bag", "container", "wishlist")) {
            return PageCapability.CONTAINER;
        }
        if (containsAny(normalized, "details", "detail", "product", "item", "record", "profile")) {
            return PageCapability.RECORD_DETAILS;
        }
        if (containsAny(normalized, "catalog", "listing", "list", "table", "search", "results")) {
            return PageCapability.RECORD_LIST;
        }
        if (containsAny(normalized, "form", "submit", "field", "input", "create", "update", "edit")) {
            return PageCapability.FORM;
        }
        return PageCapability.NAVIGATION;
    }

    private String resolvedPageName(String route, PageCapability capability) {
        if (capability == PageCapability.DASHBOARD) {
            return "DashboardPage";
        }
        if (capability == PageCapability.AUTHENTICATED_AREA && normalize(route).contains("dashboard")) {
            return "DashboardPage";
        }
        if (capability == PageCapability.RECORD_LIST
                || capability == PageCapability.RECORD_DETAILS
                || capability == PageCapability.CONTAINER
                || capability == PageCapability.FORM) {
            String evidenceName = pageNameFromRoute(route, capability);
            if (!evidenceName.isBlank()) {
                return evidenceName;
            }
        }
        return capability.defaultPageName();
    }

    private String pageNameFromRoute(String route, PageCapability capability) {
        String token = lastRouteToken(route);
        if (token.isBlank() || isGenericRouteToken(token)) {
            return "";
        }
        List<String> words = splitWords(token).stream()
                .filter(word -> !isGenericRouteToken(word))
                .filter(word -> !isViewActionPrefix(word))
                .toList();
        if (words.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        String base = builder.toString();
        if (capability == PageCapability.RECORD_LIST && !base.endsWith("List")) {
            base += "List";
        }
        if (capability == PageCapability.RECORD_DETAILS && !base.endsWith("Details") && !base.endsWith("Detail")) {
            base += "Details";
        }
        return base + "Page";
    }

    private String lastRouteToken(String route) {
        String safeRoute = safe(route);
        if (safeRoute.startsWith("http://") || safeRoute.startsWith("https://")) {
            try {
                safeRoute = java.net.URI.create(safeRoute).getPath();
            } catch (Exception ignored) {
                // Keep original route text.
            }
        }
        String[] segments = safeRoute.replaceAll("/+$", "").split("/");
        for (int index = segments.length - 1; index >= 0; index--) {
            String segment = segments[index];
            if (segment != null && !segment.isBlank()) {
                return segment;
            }
        }
        return "";
    }

    private List<String> splitWords(String token) {
        String spaced = safe(token)
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim();
        if (spaced.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(spaced.split("\\s+"))
                .map(word -> word.toLowerCase(Locale.ROOT))
                .filter(word -> !word.isBlank())
                .toList();
    }

    private boolean isViewActionPrefix(String token) {
        String normalized = normalize(token);
        return normalized.equals("view")
                || normalized.equals("show")
                || normalized.equals("open")
                || normalized.equals("get")
                || normalized.equals("edit")
                || normalized.equals("update")
                || normalized.equals("create");
    }

    private boolean isGenericRouteToken(String token) {
        String normalized = normalize(token);
        return normalized.isBlank()
                || containsAny(normalized, "index", "home", "page", "pages", "www", "project")
                || normalized.matches("[0-9]+")
                || normalized.startsWith(":")
                || normalized.startsWith("{");
    }

    private String requirementText(NormalizedRequirement requirement) {
        if (requirement == null) {
            return "";
        }
        return String.join(" ",
                safe(requirement.title()),
                safe(requirement.statement()),
                safe(requirement.expectedResult()),
                String.join(" ", requirement.tags() == null ? List.of() : requirement.tags()));
    }

    private boolean isExplicitRequirementRoute(String text, int routeStart, String route) {
        if (route == null || route.isBlank()) {
            return false;
        }
        String normalizedRoute = route.trim();
        if (normalizedRoute.equals("/") || normalizedRoute.startsWith("//")) {
            return false;
        }
        String prefix = text == null || routeStart <= 0
                ? ""
                : text.substring(Math.max(0, routeStart - 48), routeStart).toLowerCase(Locale.ROOT);
        boolean explicitContext = containsAny(prefix,
                "route",
                "url",
                "uri",
                "path",
                "redirect",
                "navigate",
                "open",
                "current url",
                "url contains",
                "route matches",
                "target route");
        return explicitContext || routeSegments(normalizedRoute) >= 2;
    }

    private int routeSegments(String route) {
        String normalized = route == null ? "" : route.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        if (normalized.isBlank()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(normalized.split("/"))
                .filter(segment -> !segment.isBlank())
                .count();
    }

    private String sourceLine(NormalizedRequirement requirement) {
        if (requirement == null || requirement.sourceReference() == null) {
            return "";
        }
        return requirement.sourceReference().source() + ":L" + requirement.sourceReference().startLine();
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
