package ua.demo.agentlab.ai.ui.prompt.scope;

public record PromptReadyAssertion(
        String type,
        String expectedValue,
        String ownerPage,
        String targetLocatorId,
        String sourceTrace,
        double confidence
) {
    public PromptReadyAssertion(
            String type,
            String expectedValue,
            String ownerPage,
            String sourceTrace,
            double confidence
    ) {
        this(type, expectedValue, ownerPage, "", sourceTrace, confidence);
    }

    public PromptReadyAssertion {
        type = safe(type);
        expectedValue = safe(expectedValue);
        ownerPage = safe(ownerPage);
        targetLocatorId = safe(targetLocatorId);
        sourceTrace = safe(sourceTrace);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
