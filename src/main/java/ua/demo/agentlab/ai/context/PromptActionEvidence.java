package ua.demo.agentlab.ai.context;

public record PromptActionEvidence(
        String name,
        String type,
        String ownerPage,
        String sourceTrace
) {
    public PromptActionEvidence {
        name = safe(name);
        type = safe(type);
        ownerPage = safe(ownerPage);
        sourceTrace = safe(sourceTrace);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
