package ua.demo.agentlab.artifactreuse.semantic;

public record FlowSemanticIndexResult(
        boolean attempted,
        boolean success,
        int indexedCount,
        String backend,
        String message
) {
    public FlowSemanticIndexResult {
        indexedCount = Math.max(0, indexedCount);
        backend = backend == null ? "" : backend.trim();
        message = message == null ? "" : message.trim();
    }

    public static FlowSemanticIndexResult skipped(String message) {
        return new FlowSemanticIndexResult(false, false, 0, "qdrant", message);
    }
}
