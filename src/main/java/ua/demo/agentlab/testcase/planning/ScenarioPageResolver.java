package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.catalog.ConfirmedPageCandidate;
import ua.demo.agentlab.ui.catalog.ConfirmedPageRegistry;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.Locale;
import java.util.Optional;

class ScenarioPageResolver {

    private final ProjectProfile profile;
    private final MappedUiKnowledge knowledge;
    private final ConfirmedPageRegistry confirmedPages;

    ScenarioPageResolver(ProjectProfile profile, MappedUiKnowledge knowledge) {
        this(profile, knowledge, null);
    }

    ScenarioPageResolver(ProjectProfile profile, MappedUiKnowledge knowledge, NormalizedRequirementBundle requirements) {
        this.profile = profile;
        this.knowledge = knowledge;
        this.confirmedPages = new ConfirmedPageSourceResolver()
                .resolve(profile, requirements, stableMappedPages(knowledge));
    }

    String routeFor(RequirementCapability capability) {
        return confirmedPageFor(capability)
                .map(ConfirmedPageCandidate::route)
                .orElse("");
    }

    String pageFor(RequirementCapability capability) {
        String route = routeFor(capability);
        String mapped = pageNameForRoute(route);
        if (!mapped.isBlank()) {
            return mapped;
        }
        return confirmedPageFor(capability)
                .map(ConfirmedPageCandidate::pageName)
                .filter(value -> !value.isBlank())
                .orElse("GenericPage");
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

    private Optional<ConfirmedPageCandidate> confirmedPageFor(RequirementCapability capability) {
        if (confirmedPages == null) {
            return Optional.empty();
        }
        return switch (capability) {
            case AUTHENTICATION -> confirmedPages.findByCapability(PageCapability.AUTHENTICATION);
            case AUTHENTICATED_AREA, LOGOUT -> confirmedPages.findByCapability(PageCapability.DASHBOARD)
                    .or(() -> confirmedPages.findByCapability(PageCapability.AUTHENTICATED_AREA))
                    .or(() -> confirmedPages.findByCapability(PageCapability.SECURITY));
            case FORM -> confirmedPages.findByCapability(PageCapability.FORM)
                    .or(() -> confirmedPages.findByCapability(PageCapability.AUTHENTICATION));
            case NAVIGATION -> confirmedPages.findByCapability(PageCapability.NAVIGATION)
                    .or(() -> confirmedPages.findByCapability(PageCapability.AUTHENTICATION));
            case RECORD_LIST -> confirmedPages.findByCapability(PageCapability.RECORD_LIST);
            case RECORD_DETAILS -> confirmedPages.findByCapability(PageCapability.RECORD_DETAILS);
            case CONTAINER -> confirmedPages.findByCapability(PageCapability.CONTAINER);
            case GENERIC -> confirmedPages.allPages().stream().findFirst();
        };
    }

    private java.util.List<ConfirmedPageCandidate> stableMappedPages(MappedUiKnowledge knowledge) {
        if (knowledge == null || knowledge.pages() == null) {
            return java.util.List.of();
        }
        return knowledge.pages().stream()
                .filter(page -> page != null && (!page.urlPattern().isBlank() || !page.url().isBlank()))
                .map(page -> new ConfirmedPageCandidate(
                        page.pageName(),
                        firstNonBlank(page.urlPattern(), page.url()),
                        toCapability(page),
                        ua.demo.agentlab.ui.catalog.PageSource.DISCOVERY_SNAPSHOT,
                        0.88d,
                        java.util.List.of("mapped-ui-knowledge:" + page.pageId())
                ))
                .toList();
    }

    private PageCapability toCapability(MappedPage page) {
        if (page == null || page.canonicalPageType() == null) {
            return PageCapability.GENERIC;
        }
        return switch (page.canonicalPageType()) {
            case AUTHENTICATION -> PageCapability.AUTHENTICATION;
            case AUTHENTICATED_AREA -> PageCapability.AUTHENTICATED_AREA;
            case DASHBOARD -> PageCapability.DASHBOARD;
            case FORM -> PageCapability.FORM;
            case REGISTRATION -> PageCapability.REGISTRATION;
            case RECOVERY -> PageCapability.RECOVERY;
            case SECURITY -> PageCapability.SECURITY;
            case LISTING -> PageCapability.RECORD_LIST;
            case SEARCH -> PageCapability.RECORD_LIST;
            case DETAILS -> PageCapability.RECORD_DETAILS;
            case CART -> PageCapability.CONTAINER;
            case LANDING -> PageCapability.NAVIGATION;
            case GENERIC -> PageCapability.GENERIC;
        };
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
