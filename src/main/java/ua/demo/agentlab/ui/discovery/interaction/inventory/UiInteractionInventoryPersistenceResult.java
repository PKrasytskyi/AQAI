package ua.demo.agentlab.ui.discovery.interaction.inventory;

public record UiInteractionInventoryPersistenceResult(
        boolean executed,
        int pageCount,
        int componentCount,
        int locatorCount,
        int actionCount,
        String details
) {
    public UiInteractionInventoryPersistenceResult {
        details = details == null ? "" : details.trim();
    }

    public static UiInteractionInventoryPersistenceResult skipped(String details) {
        return new UiInteractionInventoryPersistenceResult(false, 0, 0, 0, 0, details);
    }
}
