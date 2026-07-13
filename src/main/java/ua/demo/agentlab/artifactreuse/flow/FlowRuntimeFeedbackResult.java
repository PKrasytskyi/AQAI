package ua.demo.agentlab.artifactreuse.flow;

public record FlowRuntimeFeedbackResult(
        boolean attempted,
        boolean success,
        String backend,
        int contractsUpdated,
        String message
) {
    public FlowRuntimeFeedbackResult {
        backend = backend == null ? "" : backend.trim();
        contractsUpdated = Math.max(0, contractsUpdated);
        message = message == null ? "" : message.trim();
    }

    public static FlowRuntimeFeedbackResult skipped(String backend, String message) {
        return new FlowRuntimeFeedbackResult(false, false, backend, 0, message);
    }

    public static FlowRuntimeFeedbackResult success(String backend, int count, String message) {
        return new FlowRuntimeFeedbackResult(true, true, backend, count, message);
    }

    public static FlowRuntimeFeedbackResult failed(String backend, String message) {
        return new FlowRuntimeFeedbackResult(true, false, backend, 0, message);
    }
}
