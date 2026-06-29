package ua.demo.agentlab.ai.expectationenrichment.model;

public record ExpectedResultCandidate(
        String requirementId,
        String expectedResult,
        String sourceReference
) {
    public ExpectedResultCandidate {
        requirementId = safe(requirementId);
        expectedResult = safe(expectedResult);
        sourceReference = safe(sourceReference);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
