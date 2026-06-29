package ua.demo.agentlab.ai.assertions.model;

public record AssertionContract(
        String requirementId,
        String testCaseId,
        AssertionType type,
        String expectedValue,
        String ownerPage,
        String route,
        String sourceLine,
        double confidence,
        AssertionSource source
) {
    public AssertionContract {
        requirementId = safe(requirementId);
        testCaseId = safe(testCaseId);
        type = type == null ? AssertionType.ELEMENT_VISIBLE : type;
        expectedValue = safe(expectedValue);
        ownerPage = safe(ownerPage);
        route = safe(route);
        sourceLine = safe(sourceLine);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        source = source == null ? AssertionSource.RULE_BASED : source;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
