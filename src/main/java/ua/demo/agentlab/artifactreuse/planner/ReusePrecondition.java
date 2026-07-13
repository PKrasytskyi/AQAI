package ua.demo.agentlab.artifactreuse.planner;

public record ReusePrecondition(
        ReuseKnowledgeType type,
        String id,
        ReuseDecisionType decision,
        double confidence,
        String reason
) {
    public ReusePrecondition {
        type = type == null ? ReuseKnowledgeType.CAPABILITY : type;
        id = safe(id);
        decision = decision == null ? ReuseDecisionType.NEEDS_REVIEW : decision;
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        reason = safe(reason);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
