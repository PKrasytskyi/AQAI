package ua.demo.agentlab.ui.discovery.persistence.knowledge;

public record PageKnowledgeWriteResult(
        String target,
        boolean executed,
        int nodeCount,
        int edgeCount,
        int documentCount,
        String details
) {
    public PageKnowledgeWriteResult {
        target = target == null ? "" : target.trim();
        details = details == null ? "" : details.trim();
    }
}
