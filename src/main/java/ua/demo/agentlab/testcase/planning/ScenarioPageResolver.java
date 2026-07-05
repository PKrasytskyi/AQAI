package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.Locale;

class ScenarioPageResolver {

    private final ProjectProfile profile;
    private final MappedUiKnowledge knowledge;

    ScenarioPageResolver(ProjectProfile profile, MappedUiKnowledge knowledge) {
        this.profile = profile;
        this.knowledge = knowledge;
    }

    String routeFor(RequirementCapability capability) {
        if (profile == null) {
            return "";
        }
        return switch (capability) {
            case AUTHENTICATION, FORM -> firstNonBlank(profile.loginRoute(), profile.homeRoute());
            case AUTHENTICATED_AREA, LOGOUT -> firstNonBlank(profile.authenticatedRoute(), profile.securityRoute());
            case NAVIGATION -> firstNonBlank(profile.homeRoute(), profile.loginRoute());
            case RECORD_LIST -> firstNonBlank(profile.catalogRoute(), profile.productsRoute(), profile.homeRoute());
            case RECORD_DETAILS -> profile.detailsRoute();
            case CONTAINER -> profile.cartRoute();
            case GENERIC -> firstNonBlank(profile.homeRoute(), profile.loginRoute(), profile.authenticatedRoute());
        };
    }

    String pageFor(RequirementCapability capability) {
        String route = routeFor(capability);
        String mapped = pageNameForRoute(route);
        if (!mapped.isBlank()) {
            return mapped;
        }
        return switch (capability) {
            case AUTHENTICATION, FORM -> "LoginPage";
            case AUTHENTICATED_AREA, LOGOUT -> "AuthenticatedAreaPage";
            case NAVIGATION -> "HomePage";
            case RECORD_LIST -> "RecordListPage";
            case RECORD_DETAILS -> "RecordDetailsPage";
            case CONTAINER -> "ContainerPage";
            case GENERIC -> "Page";
        };
    }

    String pageNameForRoute(String route) {
        if (route == null || route.isBlank() || knowledge == null) {
            return "";
        }
        String normalizedRoute = normalize(route);
        return knowledge.pages().stream()
                .filter(page -> normalize(page.urlPattern()).equals(normalizedRoute)
                        || normalize(page.url()).endsWith(normalizedRoute))
                .map(MappedPage::pageName)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
