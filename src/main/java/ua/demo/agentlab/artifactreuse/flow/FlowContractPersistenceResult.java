package ua.demo.agentlab.artifactreuse.flow;

public record FlowContractPersistenceResult(
        boolean attempted,
        boolean success,
        String backend,
        int contractsPersisted,
        String message
) {
    public static FlowContractPersistenceResult skipped(String backend, String message) {
        return new FlowContractPersistenceResult(false, false, backend, 0, safe(message));
    }

    public static FlowContractPersistenceResult success(String backend, int contractsPersisted, String message) {
        return new FlowContractPersistenceResult(true, true, backend, Math.max(0, contractsPersisted), safe(message));
    }

    public static FlowContractPersistenceResult failed(String backend, String message) {
        return new FlowContractPersistenceResult(true, false, backend, 0, safe(message));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
