package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentActionDependency;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentInteractionGraph;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Builds only explicit UI-action prerequisites. It deliberately does not infer cross-page
 * business flows: a dependency is valid only when both controls exist on the same SPA page.
 */
public class ComponentInteractionGraphBuilder {

    public ComponentInteractionGraph build(SpaInventoryBundle inventory) {
        if (inventory == null || inventory.pages().isEmpty()) {
            return ComponentInteractionGraph.empty(null, "spa-component-graph:no-inventory");
        }
        List<ComponentActionDependency> dependencies = new ArrayList<>();
        for (SpaPageInventory page : inventory.pages()) {
            List<ActionRef> actions = page.components().stream()
                    .flatMap(component -> component.actions().stream().map(action -> new ActionRef(component, action)))
                    .toList();
            for (ActionRef logout : actions.stream().filter(action -> "LOGOUT".equals(action.intent())).toList()) {
                actions.stream()
                        .filter(action -> "OPEN_MENU".equals(action.intent()))
                        .max(Comparator.comparingInt(ActionRef::openerPriority)
                                .thenComparingDouble(ActionRef::confidence))
                        .ifPresent(menu -> dependencies.add(new ComponentActionDependency(
                                page.pageId(),
                                logout.component().componentId(),
                                menu.action().actionId(),
                                logout.action().actionId(),
                                "logout control must become visible after opening the user menu",
                                Math.min(menu.confidence(), logout.confidence()),
                                List.of("component-interaction:open-menu-before-logout")
                        )));
            }
        }
        return new ComponentInteractionGraph(
                ComponentInteractionGraph.SCHEMA_VERSION,
                inventory.pages().get(0).runMetadata(),
                dependencies.stream().sorted(Comparator.comparing(ComponentActionDependency::pageId)
                        .thenComparing(ComponentActionDependency::dependentActionId)).toList(),
                List.of("component-interaction:deterministic", "dependency-count=" + dependencies.size())
        );
    }

    private record ActionRef(SemanticComponentInventory component, CandidateActionEvidence action) {
        private String intent() {
            return action.intent() == null ? "" : action.intent().trim().toUpperCase(Locale.ROOT);
        }

        private double confidence() {
            return action.confidence();
        }

        private int openerPriority() {
            String target = action.targetElementId() == null
                    ? "" : action.targetElementId().toLowerCase(Locale.ROOT);
            int priority = target.contains("trigger") || target.contains("toggle") ? 4 : 0;
            if (component.type() == ua.demo.agentlab.ui.discovery.component.model.ComponentType.HEADER) {
                priority += 2;
            }
            if (target.contains("logout") || target.contains("password") || target.contains("support")) {
                priority -= 4;
            }
            return priority;
        }
    }
}
