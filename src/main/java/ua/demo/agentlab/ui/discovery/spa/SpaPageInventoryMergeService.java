package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Merges a live same-route state without discarding stronger multi-run discovery evidence. */
public final class SpaPageInventoryMergeService {

    public SpaPageInventory merge(SpaPageInventory discovered, SpaPageInventory liveTarget) {
        if (discovered == null) return liveTarget;
        if (liveTarget == null) return discovered;
        Map<String, SemanticComponentInventory> components = new LinkedHashMap<>();
        discovered.components().forEach(component -> components.put(component.componentId(), component));
        liveTarget.components().forEach(component -> components.merge(
                component.componentId(), component, this::mergeComponent));
        return new SpaPageInventory(
                first(discovered.pageId(), liveTarget.pageId()),
                first(discovered.pageName(), liveTarget.pageName()),
                first(discovered.route(), liveTarget.route()),
                mergeCapabilities(discovered.capability(), liveTarget.capability()),
                first(discovered.pageFingerprintHash(), liveTarget.pageFingerprintHash()),
                discovered.runMetadata() == null ? liveTarget.runMetadata() : discovered.runMetadata(),
                List.copyOf(components.values()),
                union(discovered.sourceTrace(), liveTarget.sourceTrace(), "spa-inventory:same-route-evidence-merge")
        );
    }

    private SemanticComponentInventory mergeComponent(
            SemanticComponentInventory discovered,
            SemanticComponentInventory liveTarget
    ) {
        Map<String, CandidateLocatorEvidence> locators = new LinkedHashMap<>();
        discovered.locators().forEach(locator -> locators.put(locatorKey(locator), locator));
        liveTarget.locators().forEach(locator -> locators.merge(locatorKey(locator), locator, this::stronger));

        Map<String, CandidateActionEvidence> actions = new LinkedHashMap<>();
        discovered.actions().forEach(action -> actions.put(actionKey(action), action));
        liveTarget.actions().forEach(action -> actions.putIfAbsent(actionKey(action), action));

        return new SemanticComponentInventory(
                discovered.componentId(),
                first(discovered.name(), liveTarget.name()),
                discovered.type(),
                first(discovered.rootLocatorStrategy(), liveTarget.rootLocatorStrategy()),
                first(discovered.rootLocatorValue(), liveTarget.rootLocatorValue()),
                first(discovered.parentComponentId(), liveTarget.parentComponentId()),
                union(discovered.elementIds(), liveTarget.elementIds()),
                Math.max(discovered.confidence(), liveTarget.confidence()),
                locators.values().stream()
                        .sorted(Comparator.comparingDouble(CandidateLocatorEvidence::qualityScore).reversed())
                        .toList(),
                List.copyOf(actions.values()),
                union(discovered.risks(), liveTarget.risks()),
                union(discovered.sourceTrace(), liveTarget.sourceTrace(), "component:same-route-evidence-merge")
        );
    }

    private CandidateLocatorEvidence stronger(CandidateLocatorEvidence left, CandidateLocatorEvidence right) {
        if (left.stableAcrossRuns() != right.stableAcrossRuns()) {
            return left.stableAcrossRuns() ? left : right;
        }
        if (left.observedEvidenceType() != right.observedEvidenceType()) {
            return evidenceRank(left) >= evidenceRank(right) ? left : right;
        }
        return left.qualityScore() >= right.qualityScore() ? left : right;
    }

    private int evidenceRank(CandidateLocatorEvidence locator) {
        return switch (locator.observedEvidenceType()) {
            case CONFIRMED_LOCATOR -> 3;
            case CANDIDATE_LOCATOR -> 2;
            case FALLBACK_LOCATOR -> 1;
        };
    }

    private String locatorKey(CandidateLocatorEvidence locator) {
        return locator.strategy().trim().toLowerCase(java.util.Locale.ROOT) + "::" + locator.value().trim();
    }

    private String actionKey(CandidateActionEvidence action) {
        return action.intent().trim().toUpperCase(java.util.Locale.ROOT) + "::"
                + action.targetElementId().replaceFirst("-\\d+$", "");
    }

    private String mergeCapabilities(String left, String right) {
        Set<String> values = new LinkedHashSet<>();
        addCapabilities(values, left);
        addCapabilities(values, right);
        return String.join("|", values);
    }

    private void addCapabilities(Set<String> values, String capability) {
        if (capability == null) return;
        for (String value : capability.split("\\|")) {
            if (!value.isBlank()) values.add(value.trim());
        }
    }

    private List<String> union(List<String> left, List<String> right, String... additional) {
        Set<String> values = new LinkedHashSet<>();
        if (left != null) values.addAll(left);
        if (right != null) values.addAll(right);
        if (additional != null) values.addAll(List.of(additional));
        return List.copyOf(values);
    }

    private String first(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
