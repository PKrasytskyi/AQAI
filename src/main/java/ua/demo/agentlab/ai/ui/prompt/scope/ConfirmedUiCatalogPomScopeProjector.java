package ua.demo.agentlab.ai.ui.prompt.scope;

import ua.demo.agentlab.ui.discovery.catalog.*;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Final catalog-to-prompt projection. It consumes only confirmed primary locators. */
public final class ConfirmedUiCatalogPomScopeProjector {
    private final PromptActionSignatureResolver actionSignatures = new PromptActionSignatureResolver();
    private final PromptAssertionEvidenceBinder assertionBinder = new PromptAssertionEvidenceBinder();

    public PromptReadyPomScope project(
            ConfirmedUiCatalog catalog,
            String requestedPage
    ) {
        ConfirmedCatalogPage page = findPage(catalog, requestedPage);
        if (page == null) {
            return new PromptReadyPomScope(requestedPage, "", false, List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of("confirmed catalog has no page-owned evidence for " + requestedPage),
                    List.of(), 0.0d);
        }
        Map<String, PromptReadyLocator> locators = new LinkedHashMap<>();
        Map<String, String> semanticIdByLocator = semanticIds(page);
        for (ConfirmedCatalogComponent component : page.components()) {
            for (ConfirmedCatalogLocator locator : component.primaryLocators()) {
                if (!eligible(locator)) continue;
                String semanticId = semanticIdByLocator.getOrDefault(locator.locatorId(), locator.elementId());
                List<String> trace = new ArrayList<>(locator.sourceTrace());
                trace.add("catalog-locator-id:" + locator.locatorId());
                trace.add("catalog-projection:primary-confirmed-only");
                locators.putIfAbsent(semanticId, new PromptReadyLocator(
                        semanticId, locator.elementId(), locator.strategy(), locator.value(), "",
                        component.name(), component.type(), locator.sameOrigin(), locator.unique(), 1, 1,
                        locator.score(), LocatorEvidenceType.CONFIRMED_LOCATOR, trace));
            }
        }
        List<String> actions = page.components().stream().flatMap(component -> component.actions().stream())
                .filter(action -> !action.primaryLocatorId().isBlank())
                .map(actionSignatures::resolve)
                .filter(action -> !action.isBlank())
                .distinct().sorted().toList();
        List<PromptAssertionEvidenceBinder.Binding> assertionBindings = page.assertionEvidence().stream()
                .map(assertion -> assertionBinder.bind(assertion, page.route(), List.copyOf(locators.values())))
                .toList();
        List<PromptReadyAssertion> assertions = assertionBindings.stream()
                .map(PromptAssertionEvidenceBinder.Binding::assertion)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        assertion -> assertion.type() + "|" + assertion.expectedValue() + "|" + assertion.targetLocatorId(),
                        assertion -> assertion,
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values().stream().toList();
        LinkedHashSet<String> requirementIds = new LinkedHashSet<>();
        page.components().stream().flatMap(component -> component.actions().stream())
                .flatMap(action -> action.requirementIds().stream()).forEach(requirementIds::add);
        List<String> gaps = new ArrayList<>(page.coverageGaps());
        assertionBindings.stream().map(PromptAssertionEvidenceBinder.Binding::coverageGap)
                .filter(gap -> !gap.isBlank()).forEach(gaps::add);
        if (locators.isEmpty() && !actions.isEmpty()) gaps.add("catalog actions have no confirmed primary locator");
        double confidence = locators.values().stream().mapToDouble(PromptReadyLocator::score).average().orElse(0.0d);
        boolean requiresAuthentication = page.capability().toUpperCase(java.util.Locale.ROOT)
                .contains("AUTHENTICATED");
        return new PromptReadyPomScope(page.pageName(), page.route(), requiresAuthentication,
                List.of(), List.copyOf(requirementIds), actions, assertions,
                List.copyOf(locators.values()), gaps, List.of(), confidence);
    }

    private ConfirmedCatalogPage findPage(ConfirmedUiCatalog catalog, String requestedPage) {
        if (catalog == null) return null;
        return catalog.pages().stream()
                .filter(item -> PageReferenceMatcher.matchesScenarioPage(item.pageName(), item.route(), requestedPage))
                .findFirst().orElse(null);
    }

    private Map<String, String> semanticIds(ConfirmedCatalogPage page) {
        Map<String, String> result = new LinkedHashMap<>();
        page.components().forEach(component -> component.actions().forEach(action -> {
            if (!action.primaryLocatorId().isBlank()) result.put(action.primaryLocatorId(), action.targetElementId());
            if (!action.standbyLocatorId().isBlank()) result.put(action.standbyLocatorId(), action.targetElementId());
        }));
        return result;
    }

    private boolean eligible(ConfirmedCatalogLocator locator) {
        return locator != null && "CONFIRMED_LOCATOR".equals(locator.evidenceType())
                && locator.sameOrigin() && locator.browserVerified() && locator.stable() && locator.unique()
                && !locator.value().isBlank();
    }

}
