package ua.demo.agentlab.config;

import java.util.List;

public record ProjectProfile(
        String profileId,
        String projectName,
        String baseUrl,
        String homeRoute,
        String loginRoute,
        String registrationRoute,
        String authenticatedRoute,
        String recoveryRoute,
        String detailsRoute,
        String formRoute,
        String securityRoute,
        String catalogRoute,
        String productsRoute,
        String cartRoute,
        OutputProfile outputProfile
) {
    public ProjectProfile {
        profileId = requireText(profileId, "profileId");
        projectName = requireText(projectName, "projectName");
        baseUrl = requireText(baseUrl, "baseUrl");
        homeRoute = normalizeOptionalRoute(homeRoute);
        loginRoute = normalizeOptionalRoute(loginRoute);
        registrationRoute = normalizeOptionalRoute(registrationRoute);
        authenticatedRoute = normalizeOptionalRoute(authenticatedRoute);
        recoveryRoute = normalizeOptionalRoute(recoveryRoute);
        detailsRoute = normalizeOptionalRoute(detailsRoute);
        formRoute = normalizeOptionalRoute(formRoute);
        securityRoute = normalizeOptionalRoute(securityRoute);
        catalogRoute = normalizeOptionalRoute(catalogRoute);
        productsRoute = normalizeOptionalRoute(productsRoute);
        cartRoute = normalizeOptionalRoute(cartRoute);
        if (outputProfile == null) {
            throw new IllegalArgumentException("outputProfile cannot be null");
        }
    }

    public List<String> configuredRoutes() {
        return List.of(
                        homeRoute,
                        loginRoute,
                        registrationRoute,
                        authenticatedRoute,
                        recoveryRoute,
                        detailsRoute,
                        formRoute,
                        securityRoute,
                        catalogRoute,
                        productsRoute,
                        cartRoute
                ).stream()
                .filter(route -> route != null && !route.isBlank())
                .distinct()
                .toList();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private static String normalizeOptionalRoute(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String route = value.trim();
        if (route.startsWith("http://") || route.startsWith("https://")) {
            return route;
        }
        return route.startsWith("/") ? route : "/" + route;
    }
}
