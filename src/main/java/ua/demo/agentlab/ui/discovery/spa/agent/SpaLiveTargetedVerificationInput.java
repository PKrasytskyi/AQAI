package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentInteractionGraph;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;

public record SpaLiveTargetedVerificationInput(ProjectProfile profile, UiInteractionInventory inventory,
                                                SpaTargetedVerificationResult planned,
                                                ComponentInteractionGraph interactionGraph,
                                                SourceStateBindingBundle sourceBindings) {
    public SpaLiveTargetedVerificationInput(ProjectProfile profile, UiInteractionInventory inventory,
                                            SpaTargetedVerificationResult planned,
                                            ComponentInteractionGraph interactionGraph) {
        this(profile, inventory, planned, interactionGraph, null);
    }
}
