package ua.demo.agentlab.artifactreuse.planner;

public record ReuseMissingKnowledge(
        ReuseKnowledgeType type,
        String id,
        ReuseDecisionType decision,
        String reason
) {
    public ReuseMissingKnowledge {
        type = type == null ? ReuseKnowledgeType.CAPABILITY : type;
        id = id == null ? "" : id.trim();
        decision = decision == null ? ReuseDecisionType.DISCOVER : decision;
        reason = reason == null ? "" : reason.trim();
    }
}
