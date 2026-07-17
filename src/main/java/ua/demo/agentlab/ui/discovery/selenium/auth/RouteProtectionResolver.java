package ua.demo.agentlab.ui.discovery.selenium.auth;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;

import java.util.List;
import java.util.Locale;

/** Separates explicit protected routes from public pages and live login redirects. */
public final class RouteProtectionResolver {

    public boolean isDeclaredProtected(
            ProjectProfile profile,
            SpaPageInventory page,
            List<TargetedActionVerification> actions
    ) {
        if (profile == null || page == null) {
            return false;
        }
        if (routeMatches(page.route(), profile.loginRoute()) || routeMatches(page.route(), profile.homeRoute())) {
            return false;
        }
        if (routeMatches(page.route(), profile.authenticatedRoute())
                || routeMatches(page.route(), profile.securityRoute())) {
            return true;
        }
        if (actions != null && actions.stream()
                .map(TargetedActionVerification::intent)
                .map(RouteProtectionResolver::normalize)
                .anyMatch(intent -> intent.equals("OPEN_MENU") || intent.equals("LOGOUT"))) {
            return true;
        }
        // Capability labels are mapper hypotheses and can be contaminated by a shared header or
        // footer. Only explicit profile routes, session-ending behavior, or runtime provenance can
        // force authentication before visiting a page.
        return page.sourceTrace().stream()
                .map(RouteProtectionResolver::normalize)
                .anyMatch(trace -> containsAny(trace, "PROTECTED_ROUTE", "AUTHENTICATED_TRANSITION", "REQUIRES_AUTHENTICATION"));
    }

    public boolean redirectedToLogin(ProjectProfile profile, String requestedRoute, String currentUrl) {
        if (profile == null || profile.loginRoute() == null || profile.loginRoute().isBlank()) {
            return false;
        }
        return !routeMatches(requestedRoute, profile.loginRoute())
                && routeMatches(currentUrl, profile.loginRoute());
    }

    private static boolean routeMatches(String route, String configuredRoute) {
        return configuredRoute != null && !configuredRoute.isBlank()
                && RouteCanonicalizer.routeEqualsOrSuffix(route, configuredRoute);
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
