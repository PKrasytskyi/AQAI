package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.ai.assertions.model.AssertionType;

public record ExpectedResultContract(
        String requirementId,
        AssertionType assertionType,
        String expectedValue,
        String ownerPage,
        String route,
        String sourceReference,
        double confidence
) {
    public ExpectedResultContract {
        requirementId = safe(requirementId);
        expectedValue = safe(expectedValue);
        ownerPage = safe(ownerPage);
        route = safe(route);
        sourceReference = safe(sourceReference);
        assertionType = assertionType == null ? AssertionType.ELEMENT_VISIBLE : assertionType;
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
