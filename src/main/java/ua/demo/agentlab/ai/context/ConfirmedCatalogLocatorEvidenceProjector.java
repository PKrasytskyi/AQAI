package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.catalog.ConfirmedCatalogComponent;
import ua.demo.agentlab.ui.discovery.catalog.ConfirmedCatalogLocator;
import ua.demo.agentlab.ui.discovery.catalog.ConfirmedCatalogPage;
import ua.demo.agentlab.ui.discovery.catalog.ConfirmedUiCatalog;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Projects the canonical confirmed catalog into the locator evidence contract consumed by AI context assembly. */
public final class ConfirmedCatalogLocatorEvidenceProjector {

    public List<PromptLocatorEvidence> project(ConfirmedUiCatalog catalog) {
        if (catalog == null || catalog.pages().isEmpty()) {
            return List.of();
        }
        Map<String, PromptLocatorEvidence> projected = new LinkedHashMap<>();
        for (ConfirmedCatalogPage page : catalog.pages()) {
            for (ConfirmedCatalogComponent component : page.components()) {
                for (ConfirmedCatalogLocator locator : component.primaryLocators()) {
                    if (!eligible(locator)) {
                        continue;
                    }
                    String semanticId = semanticId(component, locator);
                    List<String> trace = new ArrayList<>(locator.sourceTrace());
                    trace.add("confirmed-catalog-primary");
                    trace.add("catalog-page-id:" + page.pageId());
                    trace.add("catalog-page-name:" + page.pageName());
                    trace.add("catalog-route:" + page.route());
                    trace.add("catalog-component-id:" + component.componentId());
                    trace.add("catalog-locator-id:" + locator.locatorId());
                    projected.putIfAbsent(page.pageId() + "|" + locator.locatorId(), new PromptLocatorEvidence(
                            semanticId,
                            locator.elementId(),
                            locator.strategy(),
                            locator.value(),
                            role(semanticId, component.type()),
                            locator.elementId(),
                            "",
                            locator.sameOrigin(),
                            locator.score(),
                            component.name(),
                            component.type(),
                            locator.unique() ? 1 : -1,
                            locator.unique() ? 1 : -1,
                            locator.unique(),
                            LocatorEvidenceType.CONFIRMED_LOCATOR,
                            trace
                    ));
                }
            }
        }
        return List.copyOf(projected.values());
    }

    private boolean eligible(ConfirmedCatalogLocator locator) {
        return locator != null
                && "CONFIRMED_LOCATOR".equals(locator.evidenceType())
                && locator.sameOrigin()
                && locator.browserVerified()
                && locator.stable()
                && locator.unique()
                && !locator.value().isBlank();
    }

    private String semanticId(ConfirmedCatalogComponent component, ConfirmedCatalogLocator locator) {
        return component.actions().stream()
                .filter(action -> action.primaryLocatorId().equals(locator.locatorId()))
                .map(action -> action.targetElementId())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(locator.elementId());
    }

    private String role(String semanticId, String componentType) {
        String value = (semanticId + " " + componentType).toLowerCase(Locale.ROOT);
        if (value.contains("password") || value.contains("username") || value.contains("input") || value.contains("field")) {
            return "input";
        }
        if (value.contains("heading") || value.contains("title")) {
            return "heading";
        }
        if (value.contains("link") || value.contains("logout")) {
            return "link";
        }
        if (value.contains("button") || value.contains("trigger") || value.contains("menu")) {
            return "button";
        }
        return "unknown";
    }
}
