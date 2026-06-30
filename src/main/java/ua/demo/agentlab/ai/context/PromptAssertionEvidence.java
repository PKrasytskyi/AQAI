package ua.demo.agentlab.ai.context;

public record PromptAssertionEvidence(
        String type,
        String expectedValue,
        String ownerPage,
        String sourceTrace,
        double confidence
) {
    public PromptAssertionEvidence {
        type = safe(type);
        expectedValue = safe(expectedValue);
        ownerPage = safe(ownerPage);
        sourceTrace = safe(sourceTrace);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
