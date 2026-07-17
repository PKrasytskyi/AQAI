package ua.demo.agentlab.ui.discovery.spa;

public record SpaInventoryPersistenceResult(
        boolean executed,
        int pageCount,
        int componentCount,
        int locatorCount,
        int actionCount,
        String details
) {
    public SpaInventoryPersistenceResult {
        details = details == null ? "" : details.trim();
    }

    public static SpaInventoryPersistenceResult skipped(String details) {
        return new SpaInventoryPersistenceResult(false, 0, 0, 0, 0, details);
    }
}
