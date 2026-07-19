package ua.demo.agentlab.ui.discovery.interaction.inventory.agent;

import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventoryPersistenceResult;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;

import java.util.List;

public record UiInteractionInventoryOutput(
        UiInteractionInventory inventory,
        UiInteractionInventoryPersistenceResult persistence,
        List<String> writtenFiles
) {
    public UiInteractionInventoryOutput {
        writtenFiles = writtenFiles == null ? List.of() : List.copyOf(writtenFiles);
    }
}
