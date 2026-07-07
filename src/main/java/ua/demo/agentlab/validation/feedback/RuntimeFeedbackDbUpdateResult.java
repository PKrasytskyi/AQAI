package ua.demo.agentlab.validation.feedback;

public record RuntimeFeedbackDbUpdateResult(
        boolean executed,
        String target,
        int recordsUpdated,
        String details
) {
    public RuntimeFeedbackDbUpdateResult {
        target = target == null ? "" : target.trim();
        recordsUpdated = Math.max(0, recordsUpdated);
        details = details == null ? "" : details.trim();
    }
}
