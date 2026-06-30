package ua.demo.agentlab.api.model;

import java.util.List;

public record ApiAssertionContract(
        String requirementId,
        String testCaseId,
        String endpointId,
        int expectedStatusCode,
        List<JsonPathAssertion> bodyAssertions,
        List<HeaderAssertion> headerAssertions,
        List<SchemaAssertion> schemaAssertions,
        List<ResponseQualityAssertion> qualityAssertions
) {
    public ApiAssertionContract {
        requirementId = safe(requirementId);
        testCaseId = safe(testCaseId);
        endpointId = safe(endpointId);
        bodyAssertions = bodyAssertions == null ? List.of() : List.copyOf(bodyAssertions);
        headerAssertions = headerAssertions == null ? List.of() : List.copyOf(headerAssertions);
        schemaAssertions = schemaAssertions == null ? List.of() : List.copyOf(schemaAssertions);
        qualityAssertions = qualityAssertions == null ? List.of() : List.copyOf(qualityAssertions);
    }

    public boolean hasBodyOrSchemaAssertion() {
        return !bodyAssertions.isEmpty() || !schemaAssertions.isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
