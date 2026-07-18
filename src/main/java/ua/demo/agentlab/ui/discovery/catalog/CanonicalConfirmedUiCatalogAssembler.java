package ua.demo.agentlab.ui.discovery.catalog;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.interaction.model.InteractionEvidenceStatus;
import ua.demo.agentlab.ui.discovery.interaction.model.LocatorPromotionDecision;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceBundle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Projects canonical lifecycle decisions into a compact catalog without reclassifying evidence. */
public final class CanonicalConfirmedUiCatalogAssembler {

    public ConfirmedUiCatalog assemble(CanonicalInteractionEvidenceBundle canonical, List<AssertionContract> assertions) {
        if (canonical == null) {
            return new ConfirmedUiCatalog(ConfirmedUiCatalog.SCHEMA_VERSION, "", false, List.of(),
                    List.of("canonical interaction evidence is missing"));
        }
        Map<String, List<LocatorPromotionDecision>> byPage = new LinkedHashMap<>();
        canonical.promotionDecisions().stream()
                .filter(item -> item.status() == InteractionEvidenceStatus.CONFIRMED)
                .forEach(item -> byPage.computeIfAbsent(
                        item.interaction().scopedInteraction().candidate().pageId(), ignored -> new ArrayList<>()).add(item));
        List<ConfirmedCatalogPage> pages = byPage.values().stream()
                .map(items -> page(items, assertions))
                .sorted(Comparator.comparing(ConfirmedCatalogPage::route).thenComparing(ConfirmedCatalogPage::pageName))
                .toList();
        long requiredActions = canonical.requirementSelection().selected().stream()
                .map(item -> item.candidate().actionKey().value()).distinct().count();
        long confirmedActions = canonical.promotionDecisions().stream()
                .filter(item -> item.status() == InteractionEvidenceStatus.CONFIRMED)
                .map(item -> item.interaction().scopedInteraction().candidate().actionKey().value()).distinct().count();
        boolean complete = canonical.invariants().passed()
                && canonical.requirementSelection().unresolvedRequirements().isEmpty()
                && requiredActions > 0 && confirmedActions == requiredActions;
        return new ConfirmedUiCatalog(ConfirmedUiCatalog.SCHEMA_VERSION, canonical.runId(), complete, pages,
                List.of("projection-source=canonical-interaction-evidence",
                        "required-actions=" + requiredActions,
                        "confirmed-actions=" + confirmedActions,
                        "unresolved-requirements=" + canonical.requirementSelection().unresolvedRequirements().size()));
    }

    private ConfirmedCatalogPage page(List<LocatorPromotionDecision> items, List<AssertionContract> assertions) {
        var first = items.get(0).interaction().scopedInteraction().candidate();
        String pageName = provenance(first.provenance(), "page-name:");
        String capability = provenance(first.provenance(), "page-capability:");
        Map<String, List<LocatorPromotionDecision>> components = new LinkedHashMap<>();
        items.forEach(item -> components.computeIfAbsent(
                item.interaction().scopedInteraction().candidate().componentId(), ignored -> new ArrayList<>()).add(item));
        List<ConfirmedCatalogComponent> projectedComponents = components.entrySet().stream()
                .map(entry -> component(entry.getKey(), entry.getValue())).toList();
        List<AssertionContract> pageAssertions = assertions == null ? List.of() : assertions.stream()
                .filter(assertion -> assertion.ownerPage().equalsIgnoreCase(pageName)
                        || RouteCanonicalizer.routeEqualsOrSuffix(assertion.route(), first.route()))
                .toList();
        return new ConfirmedCatalogPage(capability, first.pageId(), pageName, first.route(),
                first.elementKey().stateId(), projectedComponents, pageAssertions, List.of());
    }

    private ConfirmedCatalogComponent component(String componentId, List<LocatorPromotionDecision> items) {
        Map<String, ConfirmedCatalogLocator> primary = new LinkedHashMap<>();
        Map<String, ConfirmedCatalogLocator> standby = new LinkedHashMap<>();
        Map<String, ConfirmedCatalogAction> actions = new LinkedHashMap<>();
        for (LocatorPromotionDecision decision : items) {
            var scoped = decision.interaction().scopedInteraction();
            var candidate = scoped.candidate();
            ConfirmedCatalogLocator locator = new ConfirmedCatalogLocator(
                    candidate.locatorEvidenceId().value(), candidate.elementKey().semanticRole(),
                    candidate.strategy(), candidate.value(), decision.interaction().finalScore().finalScore(),
                    "CONFIRMED_LOCATOR", candidate.sameOrigin(), decision.interaction().verification().locatorVerified(),
                    candidate.stableAcrossRuns(), candidate.uniqueWithinComponent(),
                    java.util.stream.Stream.concat(candidate.provenance().stream(),
                            scoped.requirementIds().stream().map(id -> "requirement-id:" + id)).distinct().toList());
            if (decision.primary()) primary.put(locator.locatorId(), locator);
            if (decision.standby()) standby.put(locator.locatorId(), locator);
            String actionId = candidate.actionKey().value();
            ConfirmedCatalogAction existing = actions.get(actionId);
            if (existing == null) {
                existing = new ConfirmedCatalogAction(actionId, candidate.action().name(),
                        candidate.elementKey().semanticRole(), "", "",
                        decision.interaction().finalScore().finalScore(), List.of(),
                        List.of("live postcondition verified"), scoped.requirementIds().stream().sorted().toList());
            }
            actions.put(actionId, replaceLocators(existing,
                    decision.primary() ? locator.locatorId() : existing.primaryLocatorId(),
                    decision.standby() ? locator.locatorId() : existing.standbyLocatorId()));
        }
        return new ConfirmedCatalogComponent(componentId, componentId, "SEMANTIC_COMPONENT",
                List.copyOf(actions.values()), List.copyOf(primary.values()), List.copyOf(standby.values()));
    }

    private ConfirmedCatalogAction replaceLocators(ConfirmedCatalogAction source, String primary, String standby) {
        return new ConfirmedCatalogAction(source.actionId(), source.intent(), source.targetElementId(), primary, standby,
                source.confidence(), source.preconditions(), source.postconditions(), source.requirementIds());
    }

    private String provenance(List<String> values, String prefix) {
        return values.stream().filter(value -> value.startsWith(prefix))
                .map(value -> value.substring(prefix.length())).findFirst().orElse("");
    }
}
