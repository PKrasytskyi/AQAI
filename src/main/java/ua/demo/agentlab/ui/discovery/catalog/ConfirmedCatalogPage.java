package ua.demo.agentlab.ui.discovery.catalog;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;

import java.util.List;

public record ConfirmedCatalogPage(
        String capability,
        String pageId,
        String pageName,
        String route,
        String stateId,
        List<ConfirmedCatalogComponent> components,
        List<AssertionContract> assertionEvidence,
        List<String> coverageGaps
) {
    public ConfirmedCatalogPage {
        capability = safe(capability);
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        stateId = safe(stateId);
        components = components == null ? List.of() : List.copyOf(components);
        assertionEvidence = assertionEvidence == null ? List.of() : List.copyOf(assertionEvidence);
        coverageGaps = coverageGaps == null ? List.of() : List.copyOf(coverageGaps);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
