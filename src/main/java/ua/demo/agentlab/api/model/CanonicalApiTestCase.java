package ua.demo.agentlab.api.model;

import java.util.List;

public record CanonicalApiTestCase(
        String id,
        String requirementId,
        String title,
        String endpointId,
        HttpMethod method,
        String path,
        String dataSetName,
        ApiOperationKind operationKind,
        ApiAssertionContract assertionContract,
        List<String> preconditions,
        List<String> risks,
        String sourceReference
) {
    public CanonicalApiTestCase {
        id = safe(id);
        requirementId = safe(requirementId);
        title = safe(title);
        endpointId = safe(endpointId);
        method = method == null ? HttpMethod.GET : method;
        path = normalizePath(path);
        dataSetName = safe(dataSetName);
        operationKind = operationKind == null ? ApiOperationKind.READ : operationKind;
        preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
        risks = risks == null ? List.of() : List.copyOf(risks);
        sourceReference = safe(sourceReference);
    }

    private static String normalizePath(String value) {
        String normalized = safe(value);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
