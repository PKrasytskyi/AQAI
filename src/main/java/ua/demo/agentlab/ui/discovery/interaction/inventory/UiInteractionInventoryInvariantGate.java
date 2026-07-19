package ua.demo.agentlab.ui.discovery.interaction.inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Rejects broken semantic identity and action-locator references at the generic inventory boundary. */
public final class UiInteractionInventoryInvariantGate {

    public UiInteractionInventoryInvariantReport validate(UiInteractionInventory inventory) {
        List<String> violations = new ArrayList<>();
        if (inventory == null) {
            violations.add("INVENTORY_MISSING");
            return report(violations);
        }
        Set<String> pageIds = new HashSet<>();
        for (UiInteractionPage page : inventory.pages()) {
            if (page.pageId().isBlank()) violations.add("PAGE_ID_MISSING");
            if (!page.pageId().isBlank() && !pageIds.add(page.pageId())) {
                violations.add("DUPLICATE_PAGE_ID:" + page.pageId());
            }
            Set<String> componentIds = new HashSet<>();
            for (var component : page.components()) {
                if (component.componentId().isBlank()) violations.add("COMPONENT_ID_MISSING:" + page.pageId());
                if (!component.componentId().isBlank() && !componentIds.add(component.componentId())) {
                    violations.add("DUPLICATE_COMPONENT_ID:" + page.pageId() + ":" + component.componentId());
                }
                Set<String> locatorIds = new HashSet<>();
                component.locators().forEach(locator -> {
                    if (locator.locatorId().isBlank()) {
                        violations.add("LOCATOR_ID_MISSING:" + component.componentId());
                    } else if (!locatorIds.add(locator.locatorId())) {
                        violations.add("DUPLICATE_LOCATOR_ID:" + locator.locatorId());
                    }
                    if (!locator.componentId().equals(component.componentId())) {
                        violations.add("LOCATOR_COMPONENT_MISMATCH:" + locator.locatorId());
                    }
                });
                Set<String> actionIds = new HashSet<>();
                component.actions().forEach(action -> {
                    if (action.actionId().isBlank()) {
                        violations.add("ACTION_ID_MISSING:" + component.componentId());
                    } else if (!actionIds.add(action.actionId())) {
                        violations.add("DUPLICATE_ACTION_ID:" + action.actionId());
                    }
                    if (!action.componentId().equals(component.componentId())) {
                        violations.add("ACTION_COMPONENT_MISMATCH:" + action.actionId());
                    }
                    action.requiredLocatorIds().stream().filter(id -> !locatorIds.contains(id))
                            .forEach(id -> violations.add("ACTION_LOCATOR_REFERENCE_MISSING:"
                                    + action.actionId() + ":" + id));
                });
            }
        }
        return report(violations);
    }

    public void enforce(UiInteractionInventory inventory) {
        UiInteractionInventoryInvariantReport report = validate(inventory);
        if (!report.passed()) {
            throw new IllegalStateException("UI interaction inventory invariant gate failed: "
                    + String.join("; ", report.violations()));
        }
    }

    private UiInteractionInventoryInvariantReport report(List<String> violations) {
        return new UiInteractionInventoryInvariantReport(
                UiInteractionInventoryInvariantReport.SCHEMA_VERSION, violations.isEmpty(), violations);
    }
}
