package ua.demo.agentlab.ai.expectationenrichment.model;

public record ResolvedExpectedResult(
        String testCaseId,
        String expectedValue,
        String sourceRequirementId,
        String source,
        double confidence,
        String status,
        String rationale
) {
    public ResolvedExpectedResult {
        testCaseId = safe(testCaseId);
        expectedValue = safe(expectedValue);
        sourceRequirementId = safe(sourceRequirementId);
        source = safe(source);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        status = safe(status);
        rationale = safe(rationale);
    }

    public boolean isApproved() {
        return "resolved".equalsIgnoreCase(status) && confidence >= 0.80d && !expectedValue.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
